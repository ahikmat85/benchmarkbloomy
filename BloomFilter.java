package com.test.me;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.BitSet;

/**
 * Implementation of a Bloom-filter, as described here:
 * http://en.wikipedia.org/wiki/Bloom_filter
 *
 * For updates and bugfixes, see http://github.com/magnuss/java-bloomfilter
 *
 * Inspired by the SimpleBloomFilter-class written by Ian Clarke. This
 * implementation provides a more evenly distributed Hash-function by using a
 * proper digest instead of the Java RNG. Many of the changes were proposed in
 * comments in his blog:
 * http://blog.locut.us/2008/01/12/a-decent-stand-alone-java-bloom-filter-implementation/
 *
 * @param <E>
 *            Object type that is to be inserted into the Bloom filter, e.g.
 *            String or Integer.
 * @author Magnus Skjegstad <magnus@skjegstad.com>
 */
public class BloomFilter<E> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private BitSet bitset;
	private int bitSetSize;
	private double bitsPerElement;
	private int expectedNumberOfFilterElements; // expected (maximum) number of
												// elements to be added
	private int numberOfAddedElements; // number of elements actually added to
										// the Bloom filter
	private int k; // number of hash functions
	static CustomHash murmurHasher = new CustomHash();

	/**
	 * Constructs an empty Bloom filter. The total length of the Bloom filter
	 * will be c*n.
	 *
	 * @param c
	 *            is the number of bits used per element.
	 * @param n
	 *            is the expected number of elements the filter will contain.
	 * @param k
	 *            is the number of hash functions used.
	 */
	public BloomFilter(double c, int n, int k) {
		this.expectedNumberOfFilterElements = n;
		this.k = k;
		this.bitsPerElement = c;
		this.bitSetSize = (int) Math.ceil(c * n);
		numberOfAddedElements = 0;
		this.bitset = new BitSet(bitSetSize);
	}

	/**
	 * Constructs an empty Bloom filter. The optimal number of hash functions
	 * (k) is estimated from the total size of the Bloom and the number of
	 * expected elements.
	 *
	 * @param bitSetSize
	 *            defines how many bits should be used in total for the filter.
	 * @param expectedNumberOElements
	 *            defines the maximum number of elements the filter is expected
	 *            to contain.
	 */
	public BloomFilter(int bitSetSize, int expectedNumberOElements) {
		this(bitSetSize / (double) expectedNumberOElements, expectedNumberOElements,
				(int) Math.round((bitSetSize / (double) expectedNumberOElements) * Math.log(2.0)));
	}

	/**
	 * Constructs an empty Bloom filter with a given false positive probability.
	 * The number of bits per element and the number of hash functions is
	 * estimated to match the false positive probability.
	 *
	 * @param falsePositiveProbability
	 *            is the desired false positive probability.
	 * @param expectedNumberOfElements
	 *            is the expected number of elements in the Bloom filter.
	 */
	public BloomFilter(double falsePositiveProbability, int expectedNumberOfElements) {
		this(Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2))) / Math.log(2), // c
																							// =
																							// k
																							// /
																							// ln(2)
				expectedNumberOfElements, (int) Math.ceil(-(Math.log(falsePositiveProbability) / Math.log(2)))); // k
																													// =
																													// ceil(-log_2(false
																													// prob.))
	}

	/**
	 * Construct a new Bloom filter based on existing Bloom filter data.
	 *
	 * @param bitSetSize
	 *            defines how many bits should be used for the filter.
	 * @param expectedNumberOfFilterElements
	 *            defines the maximum number of elements the filter is expected
	 *            to contain.
	 * @param actualNumberOfFilterElements
	 *            specifies how many elements have been inserted into the
	 *            <code>filterData</code> BitSet.
	 * @param filterData
	 *            a BitSet representing an existing Bloom filter.
	 */
	public BloomFilter(int bitSetSize, int expectedNumberOfFilterElements, int actualNumberOfFilterElements,
			BitSet filterData) {
		this(bitSetSize, expectedNumberOfFilterElements);
		this.bitset = filterData;
		this.numberOfAddedElements = actualNumberOfFilterElements;
	}

	/**
	 * Generates a digest based on the contents of a String.
	 *
	 * @param val
	 *            specifies the input data.
	 * @param charset
	 *            specifies the encoding of the input data.
	 * @return digest as long.
	 */
	public static int createHash(String val, Charset charset) {
		return createHash(val.getBytes(charset));
	}


	/**
	 * Generates a digest based on the contents of an array of bytes.
	 *
	 * @param data
	 *            specifies input data.
	 * @return digest as long.
	 */
	public static int createHash(byte[] data) {
		return createHashes(data, 1)[0];
	}

	/**
	 * Generates digests based on the contents of an array of bytes and splits
	 * the result into 4-byte int's and store them in an array. The digest
	 * function is called until the required number of int's are produced. For
	 * each call to digest a salt is prepended to the data. The salt is
	 * increased by 1 for each call.
	 *
	 * @param data
	 *            specifies input data.
	 * @param hashes
	 *            number of hashes/int's to produce.
	 * @return array of int-sized hashes
	 */
	public static int[] createHashes(byte[] data, int hashes) {

		return murmurHasher.hashSimpleLCG(data, data.length, hashes);
	}


	/**
	 * Calculates a hash code for this class.
	 * 
	 * @return hash code representing the contents of an instance of this class.
	 */
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 61 * hash + (this.bitset != null ? this.bitset.hashCode() : 0);
		hash = 61 * hash + this.expectedNumberOfFilterElements;
		hash = 61 * hash + this.bitSetSize;
		hash = 61 * hash + this.k;
		return hash;
	}

	/**
	 * Calculates the expected probability of false positives based on the
	 * number of expected filter elements and the size of the Bloom filter.
	 * <br />
	 * <br />
	 * The value returned by this method is the <i>expected</i> rate of false
	 * positives, assuming the number of inserted elements equals the number of
	 * expected elements. If the number of elements in the Bloom filter is less
	 * than the expected value, the true probability of false positives will be
	 * lower.
	 *
	 * @return expected probability of false positives.
	 */
	public double expectedFalsePositiveProbability() {
		return getFalsePositiveProbability(expectedNumberOfFilterElements);
	}

	/**
	 * Calculate the probability of a false positive given the specified number
	 * of inserted elements.
	 *
	 * @param numberOfElements
	 *            number of inserted elements.
	 * @return probability of a false positive.
	 */
	public double getFalsePositiveProbability(double numberOfElements) {
		// (1 - e^(-k * n / m)) ^ k
		return Math.pow((1 - Math.exp(-k * (double) numberOfElements / (double) bitSetSize)), k);

	}

	/**
	 * Get the current probability of a false positive. The probability is
	 * calculated from the size of the Bloom filter and the current number of
	 * elements added to it.
	 *
	 * @return probability of false positives.
	 */
	public double getFalsePositiveProbability() {
		return getFalsePositiveProbability(numberOfAddedElements);
	}

	/**
	 * Returns the value chosen for K.<br />
	 * <br />
	 * K is the optimal number of hash functions based on the size of the Bloom
	 * filter and the expected number of inserted elements.
	 *
	 * @return optimal k.
	 */
	public int getK() {
		return k;
	}

	/**
	 * Sets all bits to false in the Bloom filter.
	 */
	public void clear() {
		bitset.clear();
		numberOfAddedElements = 0;
	}

	/**
	 * Adds an array of bytes to the Bloom filter.
	 *
	 * @param bytes
	 *            array of bytes to add to the Bloom filter.
	 */
	public void add(byte[] bytes) {
		int[] hashes = createHashes(bytes, k);
		for (int hash : hashes)
			bitset.set(Math.abs(hash % bitSetSize), true);
		numberOfAddedElements++;
	}

	/**
	 * Returns true if the array of bytes could have been inserted into the
	 * Bloom filter. Use getFalsePositiveProbability() to calculate the
	 * probability of this being correct.
	 *
	 * @param bytes
	 *            array of bytes to check.
	 * @return true if the array could have been inserted into the Bloom filter.
	 */
	public boolean contains(byte[] bytes) {
		int[] hashes = createHashes(bytes, k);
		for (int hash : hashes) {
			if (!bitset.get(Math.abs(hash % bitSetSize))) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Read a single bit from the Bloom filter.
	 * 
	 * @param bit
	 *            the bit to read.
	 * @return true if the bit is set, false if it is not.
	 */
	public boolean getBit(int bit) {
		return bitset.get(bit);
	}

	/**
	 * Set a single bit in the Bloom filter.
	 * 
	 * @param bit
	 *            is the bit to set.
	 * @param value
	 *            If true, the bit is set. If false, the bit is cleared.
	 */
	public void setBit(int bit, boolean value) {
		bitset.set(bit, value);
	}

	/**
	 * Return the bit set used to store the Bloom filter.
	 * 
	 * @return bit set representing the Bloom filter.
	 */
	public BitSet getBitSet() {
		return bitset;
	}

	/**
	 * Returns the number of bits in the Bloom filter. Use count() to retrieve
	 * the number of inserted elements.
	 *
	 * @return the size of the bitset used by the Bloom filter.
	 */
	public int size() {
		return this.bitSetSize;
	}

	/**
	 * Returns the number of elements added to the Bloom filter after it was
	 * constructed or after clear() was called.
	 *
	 * @return number of elements added to the Bloom filter.
	 */
	public int count() {
		return this.numberOfAddedElements;
	}

	/**
	 * Returns the expected number of elements to be inserted into the filter.
	 * This value is the same value as the one passed to the constructor.
	 *
	 * @return expected number of elements.
	 */
	public int getExpectedNumberOfElements() {
		return expectedNumberOfFilterElements;
	}

	/**
	 * Get expected number of bits per element when the Bloom filter is full.
	 * This value is set by the constructor when the Bloom filter is created.
	 * See also getBitsPerElement().
	 *
	 * @return expected number of bits per element.
	 */
	public double getExpectedBitsPerElement() {
		return this.bitsPerElement;
	}

	/**
	 * Get actual number of bits per element based on the number of elements
	 * that have currently been inserted and the length of the Bloom filter. See
	 * also getExpectedBitsPerElement().
	 *
	 * @return number of bits per element.
	 */
	public double getBitsPerElement() {
		return this.bitSetSize / (double) numberOfAddedElements;
	}
}

class CustomHash {

	public int[] hashSimpleLCG(byte[] value, int m, int k) {
		// Java constants
		final long multiplier = 0x5DEECE66DL;
		final long addend = 0xBL;
		final long mask = (1L << 48) - 1;

		// Generate int from byte Array using the FNV hash
		int reduced = Math.abs(hashBytes(value));
		// Make number positive
		// Handle the special case: smallest negative number is itself as the
		// absolute value
		if (reduced == Integer.MIN_VALUE)
			reduced = 42;

		// Calculate hashes numbers iteratively
		int[] positions = new int[k];
		long seed = reduced;
		for (int i = 0; i < k; i++) {
			// LCG formula: x_i+1 = (multiplier * x_i + addend) mod mask
			seed = (seed * multiplier + addend) & mask;
			positions[i] = (int) (seed >>> (48 - 30)) % m;
		}
		return positions;
	}

	int hashBytes(byte a[]) {
		
		// 32 bit FNV constants. Using longs as Java does not support unsigned
		// datatypes.
		final long FNV_PRIME = 16777619;
		final long FNV_OFFSET_BASIS = 2166136261l;

		if (a == null)
			return 0;

		long result = FNV_OFFSET_BASIS;
		for (byte element : a) {
			result = (result * FNV_PRIME) & 0xFFFFFFFF;
			result ^= element;
		}

		// return Arrays.hashCode(a);
		return (int) result;
	}

}
