package eu.tsystems.mms.tic.testframework.layout;

import eu.tsystems.mms.tic.testframework.common.PropertyManager;
import eu.tsystems.mms.tic.testframework.constants.TesterraProperties;
import eu.tsystems.mms.tic.testframework.exceptions.TesterraSystemException;
import eu.tsystems.mms.tic.testframework.execution.testng.NonFunctionalAssert;
import eu.tsystems.mms.tic.testframework.internal.Constants;
import eu.tsystems.mms.tic.testframework.internal.Flags;
import eu.tsystems.mms.tic.testframework.layout.extraction.AnnotationReader;
import eu.tsystems.mms.tic.testframework.layout.matching.LayoutMatch;
import eu.tsystems.mms.tic.testframework.layout.matching.error.LayoutFeature;
import eu.tsystems.mms.tic.testframework.layout.reporting.LayoutErrorContextObject;
import eu.tsystems.mms.tic.testframework.layout.reporting.ScreenReferenceReportItem;
import eu.tsystems.mms.tic.testframework.layout.textlayout.*;
import eu.tsystems.mms.tic.testframework.report.model.context.MethodContext;
import eu.tsystems.mms.tic.testframework.report.model.context.report.Report;
import eu.tsystems.mms.tic.testframework.report.utils.ExecutionContextController;
import org.apache.commons.io.FileUtils;
import org.opencv.core.Mat;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class for handling layout checking screenshots.
 *
 * @author mibu
 */
public final class LayoutCheck {

    private static class MatchStep {
        Path referenceFileName;
        Path annotatedReferenceFileName;
        Path actualFileName;
        Path distanceFileName;
        Path annotationDataFileName;
        String consecutiveTargetImageName;
        boolean takeReferenceOnly;
    }

    /**
     * Hide Default constructor.
     */
    private LayoutCheck() {
    }

    /**
     * LayoutCheck Mode options
     *
     * @author sepr
     */
    public enum Mode {
        PIXEL, ANNOTATED
    }

    private static final double DISTANCE_NO_MATCH = 0;
    private static final int RGB_DEVIATION_PERCENT = PropertyManager.getIntProperty(TesterraProperties.LAYOUTCHECK_PIXEL_RGB_DEVIATION_PERCENT, 0);
    private static final double RGB_MAX_DEVIATION = 255;

    private static HashMap<String, Integer> runCount = new HashMap<String, Integer>();

    static {
        // ensure the folders to save the images exist
        final File folder = new File(PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_REFERENCE_PATH, "src/test/resources/" + Constants.SCREENREFERENCES_PATH + "/reference"));
        folder.mkdirs();
        final File folderdest = new File(PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_DISTANCE_PATH, "src/test/resources/" + Constants.SCREENREFERENCES_PATH + "/distance"));
        folderdest.mkdirs();
        final File folderact = new File(PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_ACTUAL_PATH, "src/test/resources/" + Constants.SCREENREFERENCES_PATH + "/actual"));
        folderact.mkdirs();
    }

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LayoutCheck.class);

    /**
     *
     * @param webDriver Web driver instance, has to implement TakesScreenshot
     * @param targetImageName filename of the reference screenshot/distance image (extended by a given prefix from the
     *                        properties)
     * @return Percents of pixels that are different
     */
    public static double run(WebDriver webDriver, String targetImageName) {
        String modeString = PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_MODE, "pixel").trim()
                .toUpperCase();
        Mode mode = Mode.valueOf(modeString);

        return run(webDriver, targetImageName, mode);
    }

    /**
     *
     * @param webDriver Web driver instance, has to implement TakesScreenshot
     * @param targetImageName filename of the reference screenshot/distance image (extended by a given prefix from the
     *                        properties
     * @param mode PIXEL or ANNOTATED
     * @return Percents of pixels that are different
     */
    public static double run(WebDriver webDriver, final String targetImageName, final Mode mode) {
        if (TakesScreenshot.class.isAssignableFrom(webDriver.getClass())) {
            return pRun((TakesScreenshot) webDriver, targetImageName, mode);
        } else {
            throw new RuntimeException("Passed WebDriver does not implement TakesScreenshot. WebDriver is of class " + webDriver.getClass());
        }
    }

    /**
     * Handles a screenshot action called from a test. Takes a new reference screenshot if the property
     * 'Testerra.layoutcheck.takereference' is set to true. Otherwise takes a screenshot and creates a new distance
     * image comparing the actual shown website to an existing reference screenshot.
     *
     * @param driver          Web driver instance
     * @param targetImageName filename of the reference screenshot/distance image (extended by a given prefix from the
     *                        properties)
     * @return Percents of pixels that are different (0 if reference is taken)
     */
    public static double run(final TakesScreenshot driver, final String targetImageName) {
        String modeString = PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_MODE, "pixel").trim()
                .toUpperCase();
        Mode mode = Mode.valueOf(modeString);

        return pRun(driver, targetImageName, mode);
    }

    /**
     * Creates directories for the images/ screenshots and creates distance image
     *
     * @param driver          WebDriver used
     * @param targetImageName name of the target image
     * @param mode            PIXEL or ANNOTATED
     * @return distance between the images
     */
    public static double run(final TakesScreenshot driver, final String targetImageName, final Mode mode) {
        return pRun(driver, targetImageName, mode);
    }

    /**
     * Matches annotations and returns
     */
    public static LayoutMatch matchAnnotations(
        final TakesScreenshot driver,
        final String targetImageName
    ) {
        final File screenshot = driver.getScreenshotAs(OutputType.FILE);
        return matchAnnotations(screenshot, targetImageName);
    }

    public static LayoutMatch matchAnnotations(
        final File screenshot,
        final String targetImageName
    ) {
        MatchStep step = prepare(screenshot, targetImageName);
        if (step.takeReferenceOnly) {
            return new LayoutMatch();
        } else {
            LayoutComparator comparator = matchAnnotations(targetImageName, step);
            return comparator.getLayoutMatch();
        }
    }

    private static LayoutComparator matchAnnotations(
        final String targetImageName,
        final MatchStep step
    ) {
        // read images
        String referenceAbsoluteFileName = step.referenceFileName.toAbsolutePath().toString();
        String annotatedAbsoluteFileName = step.annotatedReferenceFileName.toAbsolutePath().toString();
        String actualAbsoluteFileName = step.actualFileName.toAbsolutePath().toString();
        String distanceAbsoluteFileName = step.distanceFileName.toAbsolutePath().toString();
        String annotationDataAbsoluteFileName = step.annotationDataFileName.toAbsolutePath().toString();

        double distance;

        // create distance image to given reference
        LayoutComparator layoutComparator = new LayoutComparator();
        List<LayoutFeature> annotatedModeCriticalMatches;
        try {
            layoutComparator.compareImages(
                referenceAbsoluteFileName,
                annotatedAbsoluteFileName,
                actualAbsoluteFileName,
                distanceAbsoluteFileName,
                annotationDataAbsoluteFileName
            );

            // relation between error-elements and annotated elements
            distance = layoutComparator.getErrorRelation();
            annotatedModeCriticalMatches = layoutComparator.getCriticalMatches();
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw new TesterraSystemException("Error reading images", e);
        }

        LOGGER.debug(String.format("Saved distance image at '%s'.", step.distanceFileName));

        toReport(
            targetImageName,
            Mode.ANNOTATED,
            distance,
            step.referenceFileName,
            step.actualFileName,
            step.distanceFileName,
            step.annotatedReferenceFileName,
            annotatedModeCriticalMatches
        );

        return layoutComparator;
    }

    /**
     * Takes reference screenshots and prepares file paths for discrete matching modes
     */
    private static MatchStep prepare(
        final File screenshot,
        final String targetImageName
    ) {
        final MatchStep step = new MatchStep();
        String screenReferencePath = PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_REFERENCE_PATH);
        step.referenceFileName = Paths.get(screenReferencePath + "/" + String.format(
                PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_REFERENCE_NAMETEMPLATE, "Reference%s.png"),
                targetImageName
            )
        );
        step.annotationDataFileName = Paths.get(
            screenReferencePath + "/" + String.format(
                PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_ANNOTATIONDATA_NAMETEMPLATE, "Reference%s_data.json"),
                targetImageName
            )
        );
        step.annotatedReferenceFileName = Paths.get(
            screenReferencePath + "/" + String.format(
                PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_ANNOTATED_NAMETEMPLATE, "ReferenceAnnotated%s.png"),
                targetImageName
            )
        );

        String runCountModifier = "";
        if (!runCount.containsKey(targetImageName)) {
            runCount.put(targetImageName, 1);
        } else {
            Integer newCount = runCount.get(targetImageName) + 1;
            runCount.put(targetImageName, newCount);
            runCountModifier = newCount.toString();
        }

        step.takeReferenceOnly = PropertyManager.getBooleanProperty(TesterraProperties.LAYOUTCHECK_TAKEREFERENCE, false);
        if (step.takeReferenceOnly) {
            // take reference screenshot
            try {
                FileUtils.copyFile(screenshot, step.referenceFileName.toFile());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                throw new TesterraSystemException("Error when saving reference screenshot.", e);
            }
            LOGGER.debug(String.format("Saved reference screenshot at '%s'.", step.referenceFileName.toString()));
        } else {
            step.consecutiveTargetImageName = targetImageName + runCountModifier;
            step.actualFileName = Paths.get(PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_ACTUAL_PATH) + "/"
                + String.format(
                    PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_ACTUAL_NAMETEMPLATE, "Actual%s.png"),
                    step.consecutiveTargetImageName
                )
            );

            try {
                FileUtils.copyFile(screenshot, step.actualFileName.toFile());
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
                throw new TesterraSystemException("Error when saving screenshot.", e);
            }
            LOGGER.debug(String.format("Saved actual screenshot at '%s'.", step.actualFileName.toString()));

            // create distance file name
            step.distanceFileName = Paths.get(PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_DISTANCE_PATH) + "/"
                + String.format(
                    PropertyManager.getProperty(TesterraProperties.LAYOUTCHECK_DISTANCE_NAMETEMPLATE, "Distance%s.png"),
                    step.consecutiveTargetImageName
                )
            );
        }

        return step;
    }

    /**
     * Matches image pixels and returns an absolute distance value
     */
    public static double matchPixels(final TakesScreenshot driver, final String targetImageName) {
        final File screenshot = driver.getScreenshotAs(OutputType.FILE);
        return matchPixels(screenshot, targetImageName);
    }

    public static double matchPixels(final File screenshot, final String targetImageName) {
        final MatchStep step = prepare(screenshot, targetImageName);
        if (step.takeReferenceOnly) {
            return DISTANCE_NO_MATCH;
        } else {
            return matchPixels(targetImageName, step);
        }
    }

    private static double matchPixels(final String targetImageName, final MatchStep step) {
        double distance;
        try {
            // read images
            File refFile = step.referenceFileName.toFile();
            File actualFile = step.actualFileName.toFile();

            if (!refFile.exists()) {
                throw new FileNotFoundException(step.referenceFileName.toString());
            }
            if (!actualFile.exists()) {
                throw new FileNotFoundException(step.actualFileName.toString());
            }

            final BufferedImage referenceImage = ImageIO.read(refFile);
            final BufferedImage actualImage = ImageIO.read(actualFile);

            final boolean useIgnoreColor = PropertyManager.getBooleanProperty(
                TesterraProperties.LAYOUTCHECK_USE_IGNORE_COLOR,
                false
            );

            // create distance image to given reference
            distance = generateDistanceImage(
                referenceImage,
                actualImage,
                step.distanceFileName.toAbsolutePath().toString(),
                useIgnoreColor
            );
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new TesterraSystemException("Error reading images", e);
        }

        LOGGER.debug(String.format("Saved distance image at '%s'.", step.distanceFileName));

        toReport(
            step.consecutiveTargetImageName,
            Mode.PIXEL,
            distance,
            step.referenceFileName,
            step.actualFileName,
            step.distanceFileName,
            step.annotatedReferenceFileName,
            null
        );
        return distance;
    }

    /**
     * (private method) Creates directories for the images/ screenshots and creates distance image
     *
     * @param driver          WebDriver used
     * @param targetImageName name of the target image
     * @param mode            PIXEL or ANNOTATED
     * @return distance between the images
     */
    private static synchronized double pRun(
        final TakesScreenshot driver,
        final String targetImageName,
        final Mode mode
    ) {
        LOGGER.debug("Starting ScreenReferencer in " + mode.name() + " mode.");
        final File screenshot = driver.getScreenshotAs(OutputType.FILE);
        final MatchStep step = prepare(screenshot, targetImageName);

        if (step.takeReferenceOnly == false) {
            switch (mode) {
                case PIXEL:
                    return matchPixels(targetImageName, step);
                case ANNOTATED:
                    LayoutComparator layoutComparator = matchAnnotations(targetImageName, step);
                    return layoutComparator.getErrorRelation();
                default:
                    LOGGER.error("Mode" + mode.name() + "not supported");
                    throw new TesterraSystemException("Mode " + mode.name() + " not supported.");
            }
        }

        return DISTANCE_NO_MATCH;
    }

    public static float runTextLayoutErrorDetection(WebDriver driverWithWebsiteToRunTestOn, String reportImageName) {
        TextLayoutErrorDetector textLayoutErrorDetector = new TextLayoutErrorDetector();
        return pRunTextLayoutErrorDetection(driverWithWebsiteToRunTestOn, reportImageName, textLayoutErrorDetector);
    }

    public static float runTextLayoutErrorDetection(WebDriver driverWithWebsiteToRunTestOn, String reportImageName,
                                                    String[] tagsContainingText) {
        TextLayoutErrorDetector textLayoutErrorDetector = new TextLayoutErrorDetector(tagsContainingText);
        return pRunTextLayoutErrorDetection(driverWithWebsiteToRunTestOn, reportImageName, textLayoutErrorDetector);
    }

    private static float pRunTextLayoutErrorDetection(WebDriver driverWithWebsiteToRunTestOn, String reportImageName,
                                                      TextLayoutErrorDetector textLayoutErrorDetector) {
        TextLayoutScreenData screenData = textLayoutErrorDetector.captureScreenData(driverWithWebsiteToRunTestOn);

        Mat diffMat = ImageUtil.getAbsoluteDifference(screenData.getScreenshotTextColor1(),
                screenData.getScreenshotTextColor2());
        // ImageUtil.writeImage(diffMat, "diffMat.png");

        Mat greyScaleMat = ImageUtil.convertToGrey(screenData.getScreenshotNoText());
        // ImageUtil.writeImage(greyScaleMat, "grey.png");

        Mat edgesFromGrey = textLayoutErrorDetector.applySimpleEdgeFilter(greyScaleMat);
        // ImageUtil.writeImage(edgesFromGrey, "1edgesFromGrey.png");

        LineDetector lineDetector = new LineDetector();
        List<Line> lines = lineDetector.detectLines(edgesFromGrey);

        String imageDestination = "../../" + reportImageName + ".png";
        TextLayoutErrorReport textLayoutErrorReport = TextLayoutErrorReport.createReport(lines, diffMat,
                imageDestination);
//        ImageUtil.writeImage(textLayoutErrorReport.getReportMat(),
//                ReportUtils.getReportDir() + reportImageName + ".png");

        driverWithWebsiteToRunTestOn.navigate().refresh();

        int errorPixelCount = textLayoutErrorReport.getErrorPixelCount();
        int correctLinePixelCount = textLayoutErrorReport.getCorrectLinePixelCount();
        float errorRatio = textLayoutErrorReport.getErrorRatio();
        LOGGER.info("Finished text error detection. Found " + correctLinePixelCount + " correct line pixels, " +
                textLayoutErrorReport.getCorrectTextPixelCount() + " correct text pixels and " + errorPixelCount
                + " pixel errors with " +
                "both text and line overlapping. The error ratio is " + errorRatio + ".");

        double errorThreshold = PropertyManager
                .getDoubleProperty(TesterraProperties.LAYOUTCHECK_TEXT_ERRORDETECTOR_ERROR_THRESHOLD, 0);
        boolean errorThresholdExceeded = textLayoutErrorReport.isErrorThresholdExceeded(errorThreshold);

        // TODO: this is NOT correct:
        toReport(reportImageName, Mode.PIXEL, errorPixelCount, null, null, null, null, null);

        return errorRatio;
    }

    /**
     * Returns the color of a pixel at a certain position of the image
     *
     * @param image with a certain colored pixel
     * @param x     Position of the pixel
     * @param y     Position of the pixel
     * @return color code of the pixel
     */
    private static int getColorOfPixel(BufferedImage image, int x, int y) {
        return image.getRGB(x, y);
    }

    /**
     * Creates an image showing the differences of the given images and calculates the difference between the images in
     * percent.
     *
     * @param expectedImage  The expected image
     * @param actualImage    The actual image
     * @param resultFilename Filename to the save the image containing the differences
     * @return Percents of pixels that are different
     */
    private static double generateDistanceImage(
            final BufferedImage expectedImage,
            final BufferedImage actualImage,
            final String resultFilename,
            final boolean useIgnoreColor
    ) {
        // for counting the pixels that are different
        int pixelsInError = 0;
        int noOfIgnoredPixels = 0;
        // calculate the size of the distance image and create an empty image
        final Dimension distanceImageSize = calculateMaxImageSize(expectedImage, actualImage);
        final BufferedImage distanceImage = new BufferedImage(
                distanceImageSize.width, distanceImageSize.height,
                expectedImage.getType());

        Dimension expectedImageDimension = new Dimension(expectedImage.getWidth(), expectedImage.getHeight());
        Dimension actualImageDimension = new Dimension(actualImage.getWidth(), actualImage.getHeight());

        if (!actualImageDimension.equals(expectedImageDimension)) {
            NonFunctionalAssert.fail("The actual screenshot has a different size than the reference. Actual = " +
                    actualImageDimension +
                    ", Reference = " +
                    expectedImageDimension +
                    ".");
        }

        List<Rectangle> markedRectangles = null;
        boolean useExplicitRectangles = PropertyManager.getBooleanProperty(TesterraProperties.LAYOUTCHECK_USE_AREA_COLOR, false);
        if (!useIgnoreColor && useExplicitRectangles) {
            AnnotationReader annotationReader = new AnnotationReader();
            markedRectangles = annotationReader.readAnnotationDimensions(expectedImage);
            if (markedRectangles == null) {
                LOGGER.warn("No marked areas were found. This could be intentional.");
            }
        }

        if (markedRectangles == null) {
            markedRectangles = new ArrayList<Rectangle>();
            Rectangle rectangle = new Rectangle(0, 0, distanceImageSize.width, distanceImageSize.height);
            markedRectangles.add(rectangle);
        } else {
            for (int currentY = 0; currentY < distanceImage.getHeight(); currentY++) {
                for (int currentX = 0; currentX < distanceImage.getWidth(); currentX++) {
                    boolean pixelIsInsideExpectedImage = isPixelInImageBounds(expectedImage, currentX, currentY);
                    if (pixelIsInsideExpectedImage) {
                        int rgb = expectedImage.getRGB(currentX, currentY);
                        Color color = new Color(rgb);
                        color = color.darker().darker();
                        distanceImage.setRGB(currentX, currentY, color.getRGB());
                    } else {
                        distanceImage.setRGB(currentX, currentY, Color.BLUE.getRGB());
                    }
                }
            }
        }

        int ignoreColor = getColorOfPixel(expectedImage, 0, 0);

        for (Rectangle rectangle : markedRectangles) {
            for (int currentY = rectangle.y; currentY < rectangle.y + rectangle.height; currentY++) {
                for (int currentX = rectangle.x; currentX < rectangle.x + rectangle.width; currentX++) {
                    boolean pixelIsInsideExpectedImage = isPixelInImageBounds(expectedImage, currentX, currentY);
                    boolean pixelIsInsideActualImage = isPixelInImageBounds(actualImage, currentX, currentY);

                    if (pixelIsInsideExpectedImage) {
                        // draw every pixel that is available in the expected image
                        distanceImage.setRGB(currentX, currentY,
                                expectedImage.getRGB(currentX, currentY));
                    }

                    if (pixelIsInsideExpectedImage && pixelIsInsideActualImage) {
                        // if the pixel color at x,y is not equal and the pixel is not marked as 'to ignore'
                        int expectedRgb = expectedImage.getRGB(currentX, currentY);
                        int actualImageRGB = actualImage.getRGB(currentX, currentY);

                        boolean ignoredPixel = useIgnoreColor && expectedRgb == ignoreColor;

                        //

                        if (!ignoredPixel) {
                            boolean match = doRGBsMatch(expectedRgb, actualImageRGB);
                            if (!match) {
                                // mark the current pixel as error by painting it red
                                distanceImage.setRGB(currentX, currentY, Color.RED.getRGB());
                                pixelsInError++;
                            }
                        }

                        // count the ignored pixels for calculating
                        if (useIgnoreColor && expectedRgb == ignoreColor) {
                            noOfIgnoredPixels++;
                        }
                    } else {
                        // this pixel is not inside one or the other image - mark it, but not as error
                        distanceImage.setRGB(currentX, currentY, Color.BLUE.getRGB());
                    }
                }
            }
        }

        try {
            // write image to given file
            ImageIO.write(distanceImage, "PNG", new File(resultFilename));
        } catch (IOException ioe) {
            LOGGER.error(
                    String.format("An error occurred while trying to persist image to '%s'.", resultFilename),
                    ioe);
        }

        int totalPixels = 0;
        for (Rectangle rectangleToCompare : markedRectangles) {
            totalPixels += rectangleToCompare.height * rectangleToCompare.width;
        }

        // calculate and return the percentage number of pixels in error
        return ((double) pixelsInError / (totalPixels - noOfIgnoredPixels)) * 100;
    }

    public static boolean doRGBsMatch(int expectedRgb, int actualImageRGB) {
        if (expectedRgb == actualImageRGB) {
            return true;
        }

        if (RGB_DEVIATION_PERCENT > 0) {
            Color expectedColor = new Color(expectedRgb);
            Color actualColor = new Color(actualImageRGB);

            int percentR = (int) (100 * (Math.abs(expectedColor.getRed() - actualColor.getRed())) / RGB_MAX_DEVIATION);
            int percentG = (int) (100 * (Math.abs(expectedColor.getGreen() - actualColor.getGreen())) / RGB_MAX_DEVIATION);
            int percentB = (int) (100 * (Math.abs(expectedColor.getBlue() - actualColor.getBlue())) / RGB_MAX_DEVIATION);

//            LOGGER.info("RGB deviation percent: " + percentR + "/" + percentG + "/" + percentB);
            if (percentR <= RGB_DEVIATION_PERCENT && percentG <= RGB_DEVIATION_PERCENT && percentB <= RGB_DEVIATION_PERCENT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the sizes thats result from the maximum sizes of both pictures.
     *
     * @param expectedImage The expected image
     * @param actualImage   The actual image
     * @return Calculated maximum size of the images
     */
    private static Dimension calculateMaxImageSize(
            final BufferedImage expectedImage, final BufferedImage actualImage) {
        return new Dimension(Math.max(expectedImage.getWidth(),
                actualImage.getWidth()), Math.max(expectedImage.getHeight(),
                actualImage.getHeight()));
    }

    /**
     * Determines whether a pixel is within the bounds of an image.
     *
     * @param image The image
     * @param x     X-coordinate of the pixel
     * @param y     Y-coordinate of the pixel
     * @return true, if the pixel is within the images bounds, otherwise false
     */
    private static boolean isPixelInImageBounds(final BufferedImage image,
                                                final int x, final int y) {
        return (image.getWidth() > x) && (image.getHeight() > y);
    }

    private static void toReport(
        final String name,
        Mode mode,
        final double distance,
        final Path referenceScreenshotPath,
        final Path actualScreenshotPath,
        final Path distanceScreenshotPath,
        final Path annotatedReferenceScreenshotPath,
        List<LayoutFeature> annotatedModeCriticalMatches
    ) {
        // copy the screenshots to target
        copyFileToReport(referenceScreenshotPath);
        copyFileToReport(actualScreenshotPath);
        copyFileToReport(distanceScreenshotPath);

        final String relReferenceScreenshotPath = "../../" + Constants.SCREENREFERENCES_PATH + "/" + referenceScreenshotPath.getFileName().toString();
        final String relActualScreenshotPath = "../../" + Constants.SCREENREFERENCES_PATH + "/" + actualScreenshotPath.getFileName().toString();
        final String relDistanceScreenshotPath = "../../" + Constants.SCREENREFERENCES_PATH + "/" + distanceScreenshotPath.getFileName().toString();

        // TODO: replace with exportable content
        // create the report item
        ScreenReferenceReportItem scri = new ScreenReferenceReportItem(name, mode, distance,
                relReferenceScreenshotPath,
                relActualScreenshotPath, relDistanceScreenshotPath);
        if (mode == Mode.ANNOTATED) {
            copyFileToReport(annotatedReferenceScreenshotPath);
            String cleanedAnnotatedReferenceScreenshotPath = "../../" + Constants.SCREENREFERENCES_PATH + annotatedReferenceScreenshotPath.getFileName().toString();
            scri.setAnnotatedScreenshotPath(cleanedAnnotatedReferenceScreenshotPath);
            if (annotatedModeCriticalMatches.size() > 0) {
                scri.setAnnotatedModeCriticalMatches(annotatedModeCriticalMatches);
            }
        }

        LayoutErrorContextObject error = new LayoutErrorContextObject();
        error.name = name;
        error.mode = mode.name();
        error.msg = "Distance: " + distance;
        error.imageFileNameReference = relReferenceScreenshotPath;
        error.imageFileNameActual = relActualScreenshotPath;
        error.imageFileNameDistance = relDistanceScreenshotPath;
        MethodContext methodContext = ExecutionContextController.getCurrentMethodContext();
        methodContext.customErrorContextObjects.add(error);

        String msg = "Layout check failed: " + name;
        if (Flags.LAYOUTCHECK_ASSERT_NF) {
            NonFunctionalAssert.assertTrue(false, msg);
        }
        else {
            Assert.fail(msg);
        }

    }

    /**
     * Copies the given file to the report dir and returns the resulting path.
     *
     * @param from The path of the file to copy
     */
    private static void copyFileToReport(final Path from) {
        // TODO: shouldnt this be done from the core??
        final String fileName = from.getFileName().toString();
        final File references = new File(Report.REPORT_DIRECTORY, Constants.SCREENREFERENCES_PATH);
        if (!references.exists()) {
            references.mkdirs();
        }

        try {
            File fromFile = from.toFile();
            File toFile = new File(references, fileName);
            FileUtils.copyFile(fromFile, toFile);
            LOGGER.debug(String.format("Copied screenshot from '%s' to '%s'", fromFile.getAbsolutePath(),
                    toFile.getAbsolutePath()));
        } catch (IOException e) {
            LOGGER.error("Error trying to copy screenshot to report", e.getMessage());
        }
    }
}
