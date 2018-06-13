package nl.hva.miw.robot.cohort12;

import java.util.ArrayList;

import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.motor.*;
import lejos.hardware.sensor.*;
import lejos.robotics.RegulatedMotor;
import lejos.utility.Delay;

/**
 * With (an instance of) this class, a robot can move forward while avoiding all
 * objects on its way. It's possible to set the amount of objects to be passed
 * by the robot. The robot will pass the first object at the left, the second at
 * the right, the third at the left, the fourth at the right, etc.
 * 
 * @author Bjorn Goos
 *
 */

public class AvoiderDieWerkt {
	private static RegulatedMotor motorRight;
	private static RegulatedMotor motorLeft;
	private static RegulatedMotor motorOfHead;
	private static UnregulatedMotor motorOfGrip;
	private static ColorSensor colorSensor;
	private static EV3IRSensor infraRedSensor;
	private static EV3TouchSensor touchSensor;
	private static Grip balletjeGrijper = new Grip();
	private static final int UP_TO_OBJECT = 1000;
	private static final int UNTIL_OBJECT_IS_PASSED = 2;
	// A value of 40 equals approximately 15 centimeter, though this might differ
	// per sensor
	private static final int SMALLEST_DISTANCE_TO_OBJECT = 55;
	private static final int GO_CALMLY_FORWARD = 360; // 360
	private static int numberOfObjectsToBePassed = 2;
	private static ArrayList<Integer> lijstMetTachoMetingen = new ArrayList<>();

	/**
	 * Instantiate an object avoider
	 * 
	 * @param motorRight
	 *            The motor at the right of the robot
	 * @param motorLeft
	 *            The motor at the left of the robot
	 * @param motorOfHead
	 *            The motor that turns the 'head' of the robot, i.e. the place of
	 *            the robot where the IR-sensor is attached to
	 * @param motorOfGrip
	 *            The motor that makes the grip open and close
	 * @param colorSensor
	 *            The sensor that measures color values
	 * @param infraRedSensor
	 *            The sensor that measures distances (and 'angles' for the Beacon)
	 */
	public AvoiderDieWerkt(RegulatedMotor motorRight, RegulatedMotor motorLeft, RegulatedMotor motorOfHead,
			UnregulatedMotor motorOfGrip, ColorSensor colorSensor, EV3IRSensor infraRedSensor,
			EV3TouchSensor touchSensor) {
		super();
		AvoiderDieWerkt.motorRight = motorRight;
		AvoiderDieWerkt.motorLeft = motorLeft;
		AvoiderDieWerkt.motorOfHead = motorOfHead;
		AvoiderDieWerkt.motorOfGrip = motorOfGrip;
		AvoiderDieWerkt.colorSensor = colorSensor;
		AvoiderDieWerkt.infraRedSensor = infraRedSensor;
		AvoiderDieWerkt.touchSensor = touchSensor;
	}

	/**
	 * In the current design, a colorSensor and motorOfGrip are not needed for
	 * object avoidance.
	 */
	public AvoiderDieWerkt(RegulatedMotor motorRight, RegulatedMotor motorLeft, RegulatedMotor motorOfHead,
			EV3IRSensor infraRedSensor, EV3TouchSensor touchSensor) {
		this(motorRight, motorLeft, motorOfHead, null, null, infraRedSensor, null);
	}

	public AvoiderDieWerkt(RegulatedMotor motorRight, RegulatedMotor motorLeft, RegulatedMotor motorOfHead,
			UnregulatedMotor motorOfGrip, EV3IRSensor infraRedSensor, EV3TouchSensor touchSensor) {
		this(motorRight, motorLeft, motorOfHead, motorOfGrip, null, infraRedSensor, null);
	}

	public AvoiderDieWerkt(RegulatedMotor motorRight, RegulatedMotor motorLeft, RegulatedMotor motorOfHead,
			ColorSensor colorSensor, EV3IRSensor infraRedSensor, EV3TouchSensor touchSensor) {
		this(motorRight, motorLeft, motorOfHead, null, colorSensor, infraRedSensor, touchSensor);
	}

	public AvoiderDieWerkt() {
		this(null, null, null, null, null, null, null);
	}

	public void useObjectAvoider(RegulatedMotor motorRight, RegulatedMotor motorLeft, RegulatedMotor motorOfHead,
			UnregulatedMotor motorOfGrip, ColorSensor colorSensor, EV3IRSensor infraRedSensor) {
		AvoiderDieWerkt.motorRight = motorRight;
		AvoiderDieWerkt.motorLeft = motorLeft;
		AvoiderDieWerkt.motorOfHead = motorOfHead;
		AvoiderDieWerkt.motorOfGrip = motorOfGrip;
		AvoiderDieWerkt.colorSensor = colorSensor;
		AvoiderDieWerkt.infraRedSensor = infraRedSensor;
	}

	/**
	 * Method to start the object avoider. Place the robot in an 90 degree angle
	 * with respect to the first object for the best results. Also, make sure the
	 * 'head' of the robot is 'straight' (i.e. lined with the robot's moving
	 * direction).
	 */
	public void run() {
		lijstMetTachoMetingen.clear();
		startObjectAvoider();
		for (Integer i : lijstMetTachoMetingen) {
			System.out.println("eerste keer");
			System.out.println(i);
		}
		scoreAGoal();
		comeBack(lijstMetTachoMetingen);
		motorLeft.close();
		motorRight.close();
		motorOfHead.close();
		infraRedSensor.close();
	}

	public void startObjectAvoider() {
		Sound.beepSequence();
		System.out.println("Press a key to start the Object Avoider");
		Button.waitForAnyPress();

		int numberOfObjectsPassed = 0;
		while (numberOfObjectsPassed < numberOfObjectsToBePassed && Button.ESCAPE.isUp()) {
			// Let the robot drive calmly towards the object, until the object is nearer
			// than the SMALLEST_DISTANCE_TO_OBJECT
			keepCalmlyGoingForward(UP_TO_OBJECT);

			// Robot faces away from the object, after which the head turns towards it, so
			// the distance to the object can still be measured
			if (numberOfObjectsPassed % 2 == 0) {
				robotTurns90DegreesTo("L");
				headTurns90DegreesTo("R");
			}
			// Every next object will be passed on the other side than the previous object.
			// Just for the fun of it, but also to make sure that the robot does not
			// 'depart' too much by passing every object at the same side
			if (numberOfObjectsPassed % 2 == 1) {
				robotTurns90DegreesTo("R");
				headTurns90DegreesTo("L");
			}

			// Now, the robot will drive more or less parallel to the object, until it can
			// no longer see it
			keepCalmlyGoingForward(UNTIL_OBJECT_IS_PASSED);

			// In case the robot is quite large, it has to drive a little bit extra, so the
			// whole robot can pass the object
			littleBitExtraForward();

			// Now the robot will turn again, so it can pass the object
			// Next, the head will turn as well, so it aims at the path of the robot again
			if (numberOfObjectsPassed % 2 == 0) {
				robotTurns90DegreesTo("R");
				headTurns90DegreesTo("L");
			}
			// Again, other way around for consecutively passed objects
			if (numberOfObjectsPassed % 2 == 1) {
				robotTurns90DegreesTo("L");
				headTurns90DegreesTo("R");
			}
			numberOfObjectsPassed++;

		}

	}

	public void playWithMarvin(long forHowLong) {
		lijstMetTachoMetingen.clear();
		motorLeft.setSpeed(600);
		motorRight.setSpeed(600);
		long startTime = System.currentTimeMillis();
		long endTime = startTime + forHowLong;
		while (System.currentTimeMillis() < endTime) {
			keepCalmlyGoingForward(UP_TO_OBJECT);
			// if (allTheTimeHasPassed(endTime)) {
			// break;
			// }
			robotTurns90DegreesTo("L");
			// headTurns90DegreesTo("R");
			// keepCalmlyGoingForward(UNTIL_OBJECT_IS_PASSED);
			// if (allTheTimeHasPassed(endTime)) {
			// break;
			// }
			// robotTurns90DegreesTo("L");
			// headTurns90DegreesTo("L");
		}
		Sound.beepSequence();
		Sound.twoBeeps();
		comeBackAfterPlaying(lijstMetTachoMetingen);
		motorLeft.close();
		motorRight.close();
		motorOfHead.close();
		infraRedSensor.close();
	}

	public static boolean allTheTimeHasPassed(long endTime) {
		return (System.currentTimeMillis() > endTime);
	}

	public static void robotTurns90DegreesTo(String direction) {
		int requiredMotorRotationFor90Degrees = 400; // negative for direction
		if (direction.equals("L")) {
			motorLeft.rotate(-requiredMotorRotationFor90Degrees, true);
			motorRight.rotate(requiredMotorRotationFor90Degrees, true);
			motorLeft.waitComplete();
			motorRight.waitComplete();
		}
		if (direction.equals("R")) {
			motorLeft.rotate(requiredMotorRotationFor90Degrees, true);
			motorRight.rotate(-requiredMotorRotationFor90Degrees, true);
			motorLeft.waitComplete();
			motorRight.waitComplete();
		}
	}

	public static void headTurns90DegreesTo(String direction) {
		int motorRotationRequiredForHeadToMakeA90DegreesTurn = 65;
		if (direction.equals("L")) {
			motorOfHead.rotate(motorRotationRequiredForHeadToMakeA90DegreesTurn, true);
			motorOfHead.waitComplete();
		}
		if (direction.equals("R")) {
			motorOfHead.rotate(-motorRotationRequiredForHeadToMakeA90DegreesTurn, true);
			motorOfHead.waitComplete();
		}
	}

	private static void keepCalmlyGoingForward(int upToObjectOrUntilObjectIsPassed) {
		if (upToObjectOrUntilObjectIsPassed == UP_TO_OBJECT) {
			int measuredDistance = upToObjectOrUntilObjectIsPassed;
			motorRight.resetTachoCount();
			motorLeft.resetTachoCount();
			while (measuredDistance > SMALLEST_DISTANCE_TO_OBJECT) {
				SensorMode distanceMeasurer = infraRedSensor.getDistanceMode();
				float[] sample = new float[distanceMeasurer.sampleSize()];
				distanceMeasurer.fetchSample(sample, 0);
				measuredDistance = (int) sample[0];
				System.out.println("Distance: " + measuredDistance);
				motorLeft.rotate(GO_CALMLY_FORWARD, true);
				motorRight.rotate(GO_CALMLY_FORWARD, true);
			}
			lijstMetTachoMetingen.add(motorRight.getTachoCount());
			lijstMetTachoMetingen.add(motorLeft.getTachoCount());
		}
		if (upToObjectOrUntilObjectIsPassed == UNTIL_OBJECT_IS_PASSED) {
			int measuredDistance = upToObjectOrUntilObjectIsPassed;
			motorRight.resetTachoCount();
			motorLeft.resetTachoCount();
			while (measuredDistance < 2 * SMALLEST_DISTANCE_TO_OBJECT) {
				SensorMode distanceMeasurer = infraRedSensor.getDistanceMode();
				float[] sample = new float[distanceMeasurer.sampleSize()];
				distanceMeasurer.fetchSample(sample, 0);
				measuredDistance = (int) sample[0];
				System.out.println("Distance: " + measuredDistance);
				motorLeft.rotate(GO_CALMLY_FORWARD, true);
				motorRight.rotate(GO_CALMLY_FORWARD, true);
			}
			lijstMetTachoMetingen.add(motorRight.getTachoCount());
			lijstMetTachoMetingen.add(motorLeft.getTachoCount());
		}
	}

	private static void littleBitExtraForward() {
		motorRight.resetTachoCount();
		motorLeft.resetTachoCount();
		int extraDistance = 1000;
		motorLeft.rotate(extraDistance, true);
		motorRight.rotate(extraDistance, true);
		motorLeft.waitComplete();
		motorRight.waitComplete();
		lijstMetTachoMetingen.add(motorRight.getTachoCount());
		lijstMetTachoMetingen.add(motorLeft.getTachoCount());
	}

	public static void setNumberOfObjectsToBePassed(int numberOfObjectsToBePassed) {
		AvoiderDieWerkt.numberOfObjectsToBePassed = numberOfObjectsToBePassed;
	}

	private static void comeBack(ArrayList<Integer> lijstMetTachoMetingen) {
		motorLeft.setSpeed(800);
		motorRight.setSpeed(800);
		robotTurns90DegreesTo("R");
		motorLeft.rotate(-(lijstMetTachoMetingen.get(10) + lijstMetTachoMetingen.get(8)), true); // 0
		motorRight.rotate(-(lijstMetTachoMetingen.get(11) + lijstMetTachoMetingen.get(9)), true); // 1
		motorLeft.waitComplete();
		motorRight.waitComplete();
		robotTurns90DegreesTo("L");
		motorLeft.rotate(-lijstMetTachoMetingen.get(6), true); // 0
		motorRight.rotate(-lijstMetTachoMetingen.get(7), true); // 1
		motorLeft.waitComplete();
		motorRight.waitComplete();
		robotTurns90DegreesTo("L");
		motorLeft.rotate(-(lijstMetTachoMetingen.get(4) + lijstMetTachoMetingen.get(2)), true); // 0
		motorRight.rotate(-(lijstMetTachoMetingen.get(5) + lijstMetTachoMetingen.get(3)), true); // 1
		motorLeft.waitComplete();
		motorRight.waitComplete();
		robotTurns90DegreesTo("R");
		motorLeft.rotate(-lijstMetTachoMetingen.get(0), true);
		motorRight.rotate(-lijstMetTachoMetingen.get(1), true);
		motorLeft.waitComplete();
		motorRight.waitComplete();
	}

	private static void comeBackAfterPlaying(ArrayList<Integer> lijstMetTachoMetingen) {
		// one playing round is one complete 'circle' of moving to an object, driving
		// past it and moving head
		// moving to an object and driving past it, adds two times two measurements to
		// the list with measurements; hence, one complete round of playing equals four
		// measurements
		int numberOfPlayingRounds = lijstMetTachoMetingen.size() / 4;
		for (int round = numberOfPlayingRounds; round > 0; round--) {
			for (int step = 1; step <= 3; step += 2) {
				motorLeft.setSpeed(800);
				motorRight.setSpeed(800);
				robotTurns90DegreesTo("R");
				motorLeft.rotate(-(lijstMetTachoMetingen.get(round * 4 - step)), true);
				motorRight.rotate(-(lijstMetTachoMetingen.get(round * 4 - step)), true);
				motorLeft.waitComplete();
				motorRight.waitComplete();
			}
		}

	}

	public static void scoreAGoal() {
		balletjeGrijper.useGrip(motorOfGrip);
		balletjeGrijper.openGrip();
		balletjeGrijper.closeGrip();
	}

	public void walkThroughLabyrinth() {
		while (Button.ESCAPE.isUp()) {
			keepCalmlyGoingForward(UP_TO_OBJECT);
			robotTurns90DegreesTo("L");
			SensorMode distanceMeasurer = infraRedSensor.getDistanceMode();
			float[] sample = new float[distanceMeasurer.sampleSize()];
			distanceMeasurer.fetchSample(sample, 0);
			int measuredDistance = (int) sample[0];
			if (measuredDistance < SMALLEST_DISTANCE_TO_OBJECT) {
				motorLeft.rotate(-800, true);
				motorRight.rotate(-800, true);
				motorLeft.waitComplete();
				motorRight.waitComplete();
			}
		}
	}
}
