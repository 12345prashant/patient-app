package com.example.patientapp;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.MatOfRect;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.core.MatOfFloat;

public class BlinkDetection {

    private static final double EAR_THRESHOLD = 0.25; // EAR threshold for blink detection
    private static final int CONSECUTIVE_BLINKS = 2; // Number of consecutive blinks to trigger task
    private int consecutiveBlinks = 0;
    private boolean isBlinking = false;
    private CascadeClassifier faceCascade;
    private CascadeClassifier eyeCascade;

    public BlinkDetection(CascadeClassifier faceCascade, CascadeClassifier eyeCascade) {
        this.faceCascade = faceCascade;
        this.eyeCascade = eyeCascade;
    }

    // Call this method to process frames for blink detection
    public void processFrame(Mat rgbaFrame, Mat grayFrame, Runnable onBlinkDetected) {
        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(grayFrame, faces);

        for (Rect face : faces.toArray()) {
            // Detect eyes within the face region
            Mat faceROI = grayFrame.submat(face);
            MatOfRect eyes = new MatOfRect();
            eyeCascade.detectMultiScale(faceROI, eyes);

            // Calculate EAR for each detected eye
            for (Rect eye : eyes.toArray()) {
                double ear = calculateEAR(eye); // Calculate EAR for this eye

                if (ear < EAR_THRESHOLD) {
                    // Blink detected (eye is closed)
                    if (!isBlinking) {
                        consecutiveBlinks++;
                        isBlinking = true;
                    }
                } else {
                    isBlinking = false; // Reset when eyes open
                }
            }
        }

        // Check if 2 consecutive blinks have occurred
        if (consecutiveBlinks >= CONSECUTIVE_BLINKS) {
            // Trigger the blink detected action
            onBlinkDetected.run();
            consecutiveBlinks = 0; // Reset after task is triggered
        }
    }

    // Calculate EAR (Eye Aspect Ratio)
    private double calculateEAR(Rect eyeRect) {
        // Placeholder for EAR calculation logic
        // EAR formula: EAR = (vertical distance between eye landmarks) / (horizontal distance between eye landmarks)
        return Math.random(); // Placeholder for EAR calculation logic
    }
}
