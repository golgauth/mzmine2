package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.io.IOException;

public class ConsoleProgress {
	
	static final String anim = "|/-\\";
	
	static void updateProgress(double progressPercentage) {
		final int width = 50; // progress bar width in chars

		System.out.print("\r[");
		int i = 0;
		for (; i <= (int)(progressPercentage*width); i++) {
			System.out.print(".");
		}
		for (; i < width; i++) {
			System.out.print(" ");
		}
		System.out.print("]");
	}
	static void updateProgressAnimated(double progressPercentage) 
			throws IOException {

		int x = (int) (100 * progressPercentage);

		String data = "\r" + anim.charAt(x % anim.length()) + " " + x;
		System.out.write(data.getBytes());
	}

	public static void main(String[] args) {
		try {
			for (double progressPercentage = 0.0; progressPercentage < 1.0; progressPercentage += 0.01) {
				updateProgress(progressPercentage);
				Thread.sleep(20);
			}
		} catch (InterruptedException e) {}
	}
}