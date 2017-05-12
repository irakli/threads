package assign4;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class Cracker {
	// Array of chars used to produce strings
	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.,-!".toCharArray();

	private final byte[] bytes;
	private final int length;
	private final int threads;
	private CountDownLatch latch;
	private String result;

	private Cracker(String code, int length, int threads) {
		this.length = length;
		this.threads = threads;
		this.latch = new CountDownLatch(1); // There's only one correct answer.
		this.bytes = hexToArray(code);

		runThreads();

		try {
			latch.await();
			// Might be a good idea to interrupt all the other threads
			// if we're at this point. But doesn't really speed up the
			// program, so I'll leave this how it is.
		} catch (InterruptedException ignored) {
		}

		System.out.println(result);
	}

	/*
	 Given a byte[] array, produces a hex String,
	 such as "234a6f". with 2 chars for each byte in the array.
	 (provided code)
	*/
	public static String hexToString(byte[] bytes) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < bytes.length; i++) {
			int val = bytes[i];
			val = val & 0xff;  // remove higher bits, sign
			if (val < 16) buff.append('0'); // leading 0
			buff.append(Integer.toString(val, 16));
		}
		return buff.toString();
	}

	/*
	 Given a string of hex byte values such as "24a26f", creates
	 a byte[] array of those values, one byte value -128..127
	 for each 2 chars.
	 (provided code)
	*/
	public static byte[] hexToArray(String hex) {
		byte[] result = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			result[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
		}
		return result;
	}

	public static void main(String[] args) {
		// If we're given only one argument that means we
		// should encode it and print out the result.
		if (args.length == 1) {
			System.out.println(hexToString(encode(args[0])));
			return;
		}

		long start = System.currentTimeMillis();
		new Cracker(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
		long end = System.currentTimeMillis();

		// Print out elapsed time.
		System.out.println("Elapsed:" + (end - start) + "ms");
	}

	/**
	 * Encodes given string with SHA algorithm.
	 * @param input string
	 * @return encoded byte[]
	 */
	private static byte[] encode(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA");
			return digest.digest(input.getBytes());
		} catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * Starts the thread with index values.
	 */
	private void runThreads() {
		for (int i = 0; i < threads; i++) {
			new Worker(i).start();
		}
	}

	private class Worker extends Thread {
		private final int index;

		public Worker(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			// System.out.println("starting thread #" + index);

			// Calculate start and end index in CHARS array.
			int start = (CHARS.length / threads) * this.index;
			int end = start + CHARS.length / threads;

			// Run recursion for each char.
			for (int i = start; i < end; i++) {
				char[] soFar = new char[length];
				soFar[0] = CHARS[i];
				crack(soFar, 1);
			}
		}

		/**
		 * Recursively generates every possible string.
		 * @param soFar char[] with starting symbol
		 * @param index current position
		 */
		private void crack(char[] soFar, int index) {
			if (index == soFar.length) {
				// If the recursion is here that means that full string
				// is already generated. We have to check its validity.
				String word = new String(soFar);
				if (Arrays.equals(bytes, encode(word))) {
					latch.countDown(); // Let the main thread know we're done here.
					result = word;
				}
				return;
			}

			// Recursive step.
			for (char ch : CHARS) {
				soFar[index] = ch;
				crack(soFar, index + 1);
			}
		}
	}

	// possible test values:
	// a 86f7e437faa5a7fce15d1ddcb9eaeaea377667b8
	// fm adeb6f2a18fe33af368d91b09587b68e3abcb9a7
	// a! 34800e15707fae815d7c90d49de44aca97e2d759
	// xyz 66b27417d37e024c46526c2f6d358a754fc552f3

}
