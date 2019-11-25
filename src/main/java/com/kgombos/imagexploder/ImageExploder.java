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
	static int range = 1;
	static boolean starLines = false;
	static int weightScale = 1;
	
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
			else if (args[index].equals("-p")) {
				index++;
				range = Integer.parseInt(args[index]);
			}
			else if (args[index].equals("-w")) {
				index++;
				weightScale = Integer.parseInt(args[index]);
			}
			index++;
		}
		
		explodeImage();
		
	}
	
	private static void explodeImage() {
		
		try {
			File input = fileInput.toFile();
			BufferedImage image = ImageIO.read(input);
			Pixel[][] pixelArray = getPixelArray(image);
			
			int oldWidth = image.getWidth();
			int oldHeight = image.getHeight();
			
			int newWidth = image.getWidth() * ratio - ratio + 1;
			int newHeight = image.getHeight() * ratio - ratio + 1;
			
			Pixel[][] newImageArray = new Pixel[newHeight][newWidth];
			
			for (int y = 0; y < oldHeight; y++) {
				for (int x = 0; x < oldWidth; x++) {
					newImageArray[y * ratio][x * ratio] = pixelArray[y][x];
				}
			}
			
			for (int y = 0; y < oldHeight; y++) {
				System.out.println("Building squares, row " + (y + 1) + " of " + oldHeight);
				for (int x = 0; x < oldWidth; x++) {
					buildSquare(newImageArray, x, y);
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
	
	private static Pixel[][] getPixelArray(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		
		Pixel[][] array = new Pixel[height][width];
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				array[y][x] = new Pixel(x * ratio, y * ratio, new Color(image.getRGB(x, y)));
			}
		}
		return array;
	}
	
	private static BufferedImage setPixelArray(Pixel[][] array, int imageType) {
		int height = array.length;
		int width = array[0].length;
		
		BufferedImage image = new BufferedImage(width, height, imageType);
		
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				image.setRGB(x, y, array[y][x].getColor().getRGB());
			}
		}
		
		return image;
		
	}
	
	private static void buildSquare(Pixel[][] newArray, int x, int y) {
		
		int yRatio = y * ratio;
		int xRatio = x * ratio;
		List<Pixel> squareList = new ArrayList<>();
		
		for (int yPos = -range; yPos <= range + 1; yPos++) {
			for (int xPos = -range; xPos <= range + 1; xPos++) {
				if (!(yRatio + yPos * ratio  < 0) && !(yRatio + yPos * ratio > newArray.length) && !(xRatio + xPos * ratio  < 0) && !(xRatio + xPos * ratio > newArray[0].length)) {
					squareList.add(newArray[yRatio + yPos * ratio][xRatio + xPos * ratio]);
				}
			}
		}
		
		for (int yPos = 0; yPos < ratio; yPos++) {
			for (int xPos = 0; xPos < ratio; xPos++) {
				if (!(xPos == 0 && yPos == 0) && !(xRatio + xPos >= newArray[0].length) && !(yRatio + yPos >= newArray.length)) {
					float[] distList = new float[squareList.size()];
					Pixel averagePixel = new Pixel(xRatio + xPos, yRatio + yPos, new Color(0));
					for (int z = 0; z < squareList.size(); z++) {
						distList[z] = averagePixel.getDist(squareList.get(z));
					}
					newArray[yRatio + yPos][xRatio + xPos] = new Pixel(xRatio + xPos, yRatio + yPos, averageColor(squareList, distList));
				}
			}
		}
		
		if (starLines) {
			drawStarLines(newArray, xRatio, yRatio);
		}
	}
	
	private static void drawStarLines(Pixel[][] newArray, int xRatio, int yRatio) {
		if (yRatio != newArray.length - 1 && xRatio != newArray[0].length - 1) {
			List<Pixel> horizontalList = new ArrayList<>();
			horizontalList.add(newArray[yRatio][xRatio]);
			horizontalList.add(newArray[yRatio][xRatio + ratio]);
			
			List<Pixel> verticalList = new ArrayList<>();
			verticalList.add(newArray[yRatio][xRatio]);
			verticalList.add(newArray[yRatio + ratio][xRatio]);
			
			for (int z = 1; z < ratio; z++) {
				float[] distList = getDistListStarLine(z);
				newArray[yRatio + z][xRatio] = new Pixel(xRatio, yRatio + z, averageColor(verticalList, distList));
				newArray[yRatio][xRatio + z] = new Pixel(xRatio + z, yRatio, averageColor(horizontalList, distList));
			}
		}
	}
	
	private static float[] getDistListStarLine(int x) {
		float[] distList = new float[2];
		distList[0] = getDist1d(0, x);
		distList[1] = getDist1d(ratio, x);
		return distList;
	}
	
	private static float getDist1d(float x, float targetX) {
		return (float) Math.abs(x - targetX);
	}
	
	private static int weightedAverage(List<Integer> values, float[] dist) {
		float distSum = 0;
		float weightSum = 0;
		for (int x = 0; x < values.size(); x++) {
			if (weightScale != 1) {
				distSum += 1 / Math.pow(dist[x], weightScale);
				weightSum += values.get(x) / Math.pow(dist[x], weightScale);
			}
			else {
				distSum += 1 / dist[x];
				weightSum += values.get(x) / dist[x];
			}
		}
		return Math.round(weightSum / distSum);
	}
	
	private static Color averageColor(List<Pixel> colors, float[] dist) {
		int averageRed = weightedAverage(
				colors.stream().map(Pixel::getColor).map(Color::getRed).collect(Collectors.toList()), dist);
		int averageBlue = weightedAverage(
				colors.stream().map(Pixel::getColor).map(Color::getBlue).collect(Collectors.toList()), dist);
		int averageGreen = weightedAverage(
				colors.stream().map(Pixel::getColor).map(Color::getGreen).collect(Collectors.toList()), dist);
		int averageAlpha = weightedAverage(
				colors.stream().map(Pixel::getColor).map(Color::getAlpha).collect(Collectors.toList()), dist);
		
		Color averageColor = new Color(averageRed, averageGreen, averageBlue, averageAlpha);
		return averageColor;
	}
	
}
