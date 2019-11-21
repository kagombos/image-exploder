package com.kgombos.imagexploder;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

public class ImageExploder {
	
	static Path fileInput;
	static Path fileOutput;
	static int ratio = 2;
	static boolean starLines = false;
	
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Input file path required");
			return;
		}
		
		int index = 1;
		fileInput = Paths.get(args[0]);
		
		while (index < args.length) {
			if (args[index].equals("-r")) {
				index++;
				ratio = Integer.parseInt(args[index]);
			}
			else if (args[index].equals("-o")) {
				index++;
				fileOutput = Paths.get(args[index]);
			}
			else if (args[index].equals("-s")) {
				starLines = true;
			}
			index++;
		}
		
		explodeImage();
		
	}
	
	private static void explodeImage() {
		
		try {
			File input = fileInput.toFile();
			BufferedImage image = ImageIO.read(input);
			int[][] pixelArray = getPixelArray(image);
			
			int oldWidth = image.getWidth();
			int oldHeight = image.getHeight();
			
			int newWidth = image.getWidth() * ratio - ratio + 1;
			int newHeight = image.getHeight() * ratio - ratio + 1;
			
			int[][] newImageArray = new int[newHeight][newWidth];
			
			for (int y = 0; y < oldHeight - 1; y++) {
				for (int x = 0; x < oldWidth - 1; x++) {
					buildSquare(pixelArray, newImageArray, x, y);
				}
			}
			BufferedImage newImage = setPixelArray(newImageArray, image.getType());
			
			File output = null;
			if (fileOutput != null) {
				output = new File(fileOutput.toString());
			}
			else {
				output = new File("exploded-" + fileInput.getFileName());				
			}
			ImageIO.write(newImage, "bmp", output);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static int[][] getPixelArray(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		
		int[][] array = new int[height][width];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				array[y][x] = image.getRGB(x, y);
			}
		}
		return array;
	}
	
	private static BufferedImage setPixelArray(int[][] array, int imageType) {
		int height = array.length;
		int width = array[0].length;
		
		BufferedImage image = new BufferedImage(width, height, imageType);
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, array[y][x]);
			}
		}
		
		return image;
		
	}
	
	private static void buildSquare(int[][] oldArray, int[][] newArray, int x, int y) {
		
		int yRatio = y * ratio;
		int xRatio = x * ratio;
		
		newArray[yRatio][xRatio] = oldArray[y][x];
		newArray[yRatio + ratio][xRatio] = oldArray[y + 1][x];
		newArray[yRatio][xRatio + ratio] = oldArray[y][x + 1];
		newArray[yRatio + ratio][xRatio + ratio] = oldArray[y + 1][x + 1];
		
		for (int yPos = 1; yPos < ratio; yPos++) {
			for (int xPos = 1; xPos < ratio; xPos++) {
				float[] distList = getDistList2d(xPos, yPos);
				newArray[yRatio + yPos][xRatio + xPos] = getAverageColor(
					new int[] {
						newArray[yRatio][xRatio], 
						newArray[yRatio + ratio][xRatio],
						newArray[yRatio][xRatio + ratio],
						newArray[yRatio + ratio][xRatio + ratio]
					},
					distList);
			}
		}
		
		if (starLines) {
			drawStarLines(newArray, xRatio, yRatio);
		}
		else {
			for (int z = 1; z < ratio; z++) {
				float[] distList = getDistList1d(z);
				buildHorizontalPixel(newArray, xRatio, yRatio, z, distList);
				buildHorizontalPixel(newArray, xRatio, yRatio + ratio, z, distList);
				buildVerticalPixel(newArray, xRatio, yRatio, z, distList);
				buildVerticalPixel(newArray, xRatio + ratio, yRatio, z, distList);
			}
		}
		
	}
	
	private static void buildHorizontalPixel(int[][] newArray, int x, int y, int z, float[] distList) {
		boolean top = true;
		boolean bottom = true;
		
		if (y == 0) {
			top = false;
		}
		else if (y == newArray.length - 1) {
			bottom = false;
		}
		
		int[] valList;
		if (top && bottom) {
			valList = new int[] {newArray[y][x], newArray[y][x + ratio], newArray[y - 1][x + z], newArray[y + 1][x + z]};
		}
		else if (top) {
			valList = new int[] {newArray[y][x], newArray[y][x + ratio], newArray[y - 1][x + z]};
		}
		else {
			valList = new int[] {newArray[y][x], newArray[y][x + ratio], newArray[y + 1][x + z]};
		}
		
		newArray[y][x + z] = getAverageColor(valList, distList);
	}
	
	private static void buildVerticalPixel(int[][] newArray, int x, int y, int z, float[] distList) {
		boolean left = true;
		boolean right = true;
		
		if (x == 0) {
			left = false;
		}
		else if (x == newArray[0].length - 1) {
			right = false;
		}
		
		int[] valList;
		if (left && right) {
			valList = new int[] {newArray[y][x], newArray[y + ratio][x], newArray[y + z][x - 1], newArray[y + z][x + 1]};
		}
		else if (left) {
			valList = new int[] {newArray[y][x], newArray[y + ratio][x], newArray[y + z][x - 1]};
		}
		else {
			valList = new int[] {newArray[y][x], newArray[y + ratio][x], newArray[y + z][x + 1]};
		}
		
		newArray[y + z][x] = getAverageColor(valList, distList);
	}
	
	private static void drawStarLines(int[][] newArray, int xRatio, int yRatio) {
		for (int z = 1; z < ratio; z++) {
			float[] distList = getDistListStarLine(z);
			newArray[yRatio + z][xRatio] = getAverageColor(new int[] {newArray[yRatio][xRatio], newArray[yRatio + ratio][xRatio]}, distList);
			newArray[yRatio + z][xRatio + ratio] = getAverageColor(new int[] {newArray[yRatio][xRatio + ratio], newArray[yRatio + ratio][xRatio + ratio]}, distList);
			newArray[yRatio][xRatio + z] = getAverageColor(new int[] {newArray[yRatio][xRatio], newArray[yRatio][xRatio + ratio]}, distList);
			newArray[yRatio + ratio][xRatio + z] = getAverageColor(new int[] {newArray[yRatio + ratio][xRatio], newArray[yRatio + ratio][xRatio + ratio]}, distList);
		}
	}
	
	private static float[] getDistListStarLine(int x) {
		float[] distList = new float[2];
		distList[0] = getDist1d(0, x);
		distList[1] = getDist1d(ratio, x);
		return distList;
	}
	
	private static float[] getDistList1d(int x) {
		float[] distList = new float[4];
		distList[0] = getDist1d(0, x);
		distList[1] = getDist1d(ratio, x);
		distList[2] = 1;
		distList[3] = 1;
		return distList;
	}
	
	private static float[] getDistList2d(int x, int y) {
		float[] distList = new float[4];
		distList[0] = getDist(0, 0, x, y);
		distList[1] = getDist(0, ratio, x, y);
		distList[2] = getDist(ratio, 0, x, y);
		distList[3] = getDist(ratio, ratio, x, y);
		return distList;
	}
	
	private static float getDist1d(float x, float targetX) {
		return (float) Math.abs(x - targetX);
	}
	
	private static float getDist(float x, float y, float targetX, float targetY) {
		return (float) Math.sqrt(Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2));
	}
	
	private static int getAverageColor(int[] colorVal, float[] dist) {
		List<Color> colors = new ArrayList<>();
		for (int x = 0; x < colorVal.length; x++) {
			colors.add(new Color(colorVal[x]));
		}
		int averageColor = averageColor(colors, dist);
		
		return averageColor;
	}
	
	private static int weightedAverage(List<Integer> values, float[] dist) {
		float distSum = 0;
		float weightSum = 0;
		for (int x = 0; x < values.size(); x++) {
			distSum += 1 / dist[x];
			weightSum += values.get(x) / dist[x];
		}
		return Math.round(weightSum / distSum);
	}
	
	private static int averageColor(List<Color> colors, float[] dist) {
		int averageRed = weightedAverage(colors.stream().map(Color::getRed).collect(Collectors.toList()), dist);
		int averageBlue = weightedAverage(colors.stream().map(Color::getBlue).collect(Collectors.toList()), dist);
		int averageGreen = weightedAverage(colors.stream().map(Color::getGreen).collect(Collectors.toList()), dist);
		int averageAlpha = weightedAverage(colors.stream().map(Color::getAlpha).collect(Collectors.toList()), dist);
		
		Color averageColor = new Color(averageRed, averageGreen, averageBlue, averageAlpha);
		return averageColor.getRGB();
	}
	
}
