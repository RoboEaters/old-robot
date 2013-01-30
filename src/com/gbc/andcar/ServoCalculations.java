package com.gbc.andcar;

import android.util.Log;

class ServoCalculations {

	// public double screenX, screenY;
	public double horizontalViewAngle, verticalViewAngle;

	public static final int MINPWx = 600;
	public static final int MAXPWx = 2500;
	public static final int MINPWy = 1500;
	public static final int MAXPWy = 2500;
	public static final int MIDDLEPW = 1550;
	public double mountPWx;
	public double mountPWy;
	public double motorPW;
	// for off screen
	private int OFFSCREENMOVE = 100;
	private double lastYPoint;
	private double lastXPoint;

	public double currentServoAngle;
	public double lastxPercent;
	public double lastyPercent;
	boolean sweepLeft = true;
	public long startTime;

	public static final int FASTFORWARDMOTOR = 1400;
	public static final int FORWARDMOTOR = 1450;
	public static final int BACKMOTOR = 1600;

	public static final int FORWARDSTOP = 1550;
	public static final int BACKSTOP = 1550;
	public static final int ACTUALSTOP = 1550;

	public static final int ZONEMIN = 20000;
	public static final int ZONEMAX = 40000;
	public static final int ZONEMIDDLE = 30000;
	public static final int AREATHRESHOLD = 20000;
	public static final int NOTBALL = 200;

	// extra motor push when the wheels are far off
	int EXTRATURNPW = 50;

	double forwardPercentMotor;
	double backwardPercentMotor;

	// wheel calculations
	int MIDWHEEL = 1400;
	int WHEELMIN = 1900;
	int WHEELMAX = 900;
	double wheelPW;

	// this is for the servos that will called in IOIO
	double[] ServoPW = new double[4];
	// scanning
	double[] scanPW = new double[2];

	// IR
	IRCalculations irc;

	// reference to creating class
	Controller controller;

	public void wheelPWCheck() {
		if (wheelPW < WHEELMIN)
			wheelPW = WHEELMIN;
		if (wheelPW > WHEELMAX)
			wheelPW = WHEELMAX;
	}

	public ServoCalculations(double horizontalViewAngle,
			double verticalViewAngle, Controller con) {

		this.verticalViewAngle = verticalViewAngle;
		this.horizontalViewAngle = horizontalViewAngle;
		controller = con;

		// set the pulse width to be exactly the middle
		mountPWx = MIDDLEPW;
		mountPWy = MAXPWy;
		motorPW = 1500;

		wheelPW = MIDWHEEL;

		lastYPoint = 0;
		lastXPoint = 0;
		currentServoAngle = 90;
		lastxPercent = 0.50; // at the half way point
		lastyPercent = 0.50; // at the half way point
		// startTime = System.nanoTime();
		// countOutOfFrameX = 0;
		// countOutOfFrameY = 0;
		// IR
		irc = new IRCalculations();
	}

	public double[] getServoPW() {
		ServoPW[0] = mountPWx;
		ServoPW[1] = mountPWy;
		ServoPW[2] = motorPW;
		ServoPW[3] = wheelPW;
		return ServoPW;
	}

	synchronized double[] getSetupInfo() {
		double[] info = new double[4];
		info[0] = MIDDLEPW;
		info[1] = MIDDLEPW;
		info[2] = ACTUALSTOP;
		info[3] = MIDWHEEL;
		return info;
	}

	public double calculateMotorPW(double currentMaxArea) {
		Log.d("SPLASH", "ENTER MOTOR CALCULATE");
		//SWITCHING THIS
		forwardPercentMotor = (1 - (currentMaxArea / AREATHRESHOLD))
				* (FORWARDMOTOR - FORWARDSTOP) + FORWARDSTOP;
		// since backwards is smaller than stoped, the back - stop will
		// always be negative
		backwardPercentMotor = (1 - (AREATHRESHOLD / currentMaxArea))
				* (BACKMOTOR - BACKSTOP) + BACKSTOP;
		// 1350-1425 + 1425

		if (currentMaxArea > AREATHRESHOLD)
			motorPW = backwardPercentMotor;
		else if (currentMaxArea < AREATHRESHOLD && (currentMaxArea > NOTBALL))
			motorPW = forwardPercentMotor;
		else if (currentMaxArea < NOTBALL) {
			// if (!((wheelPW < (WHEELMIN + 25)) || (wheelPW > (WHEELMAX -
			// 25))))//if the wheels are NOT completely turned
			motorPW = ACTUALSTOP;
		}

		Log.i("CURRENTMAXAREA", "The area is " + currentMaxArea);

		// extra motor push if car needs to turn
		double extraPW = Math.abs((mountPWx - MIDDLEPW) / (MAXPWx - MIDDLEPW)
				* EXTRATURNPW);
		motorPW = motorPW + extraPW;

		// checks
		if (motorPW < BACKMOTOR)
			motorPW = BACKMOTOR;
		// this is NOT fastforward motor
		if (motorPW > FORWARDMOTOR)
			motorPW = FORWARDMOTOR;
		return motorPW;

	}

	public void calculateWheelPW(double currentMaxArea) {
		double diffC = 1.5; // the difference constant
		double differencePercent = diffC
				* ((mountPWx - MIDDLEPW) / (MAXPWx - MIDDLEPW));
		wheelPW = (int) (differencePercent * (MIDWHEEL - WHEELMIN) + MIDWHEEL);

		if (currentMaxArea > ZONEMIDDLE) {
			double diff = wheelPW - MIDDLEPW; // pos if bigger, neg is less
			wheelPW = MIDDLEPW - diff;
		}
	}

	public double[] calculateMountPW(double screenX, double screenY,
			double currentMaxArea, double currentX, double currentY) {
		double aX = 0.33; // the constant for X
		double aY = 0.33; // the constant for Y

		double percentOfX = 0.50;
		double percentOfY = 0.50;

		if (!(currentX == 0)) {
			percentOfX = currentX / screenX;

			// set it to stand still in the middle
			if (percentOfX > 0.40 && percentOfX <= 0.50)
				percentOfX = 0.4;
			if (percentOfX < 0.60 && percentOfX >= 0.50)
				percentOfX = 0.6;

			if (lastXPoint == currentX) // If it's the same point, then it's
										// probably off the screen
			{
				if (percentOfX < 0.5)
					percentOfX = 0;
				if (percentOfX > 0.5)
					percentOfX = 1;
			}
		}

		double PWAngleX = percentOfX * horizontalViewAngle
				- (horizontalViewAngle / 2);
		mountPWx = mountPWx + aX * ((PWAngleX * 95) / 9);

		// Y DETECTION

		if (!(currentY == 0)) {
			percentOfY = currentY / screenY;

			// set it to stand still in the middle
			if (percentOfY > 0.40 && percentOfY <= 0.50)
				percentOfY = 0.5;
			if (percentOfY < 0.60 && percentOfY >= 0.50)
				percentOfY = 0.5;
		}

		double PWAngleY = percentOfY * verticalViewAngle
				- (verticalViewAngle / 2);
		mountPWy = mountPWy - aY * ((PWAngleY * 95) / 9); // THIS IS A MINUS
		checkPWLimits();

		lastYPoint = currentY;
		lastXPoint = currentX;
		// lastxPercent = percentOfX;
		// lastyPercent = currentY;
		double[] PWs = new double[2];
		PWs[0] = mountPWx;
		PWs[1] = mountPWy;
		return PWs;
	}

	public double[] scan() throws InterruptedException {
		mountPWy = 2300; // straight looking
		if (sweepLeft)
			mountPWx = mountPWx - 100;
		else if (!sweepLeft)
			mountPWx = mountPWx + 100;

		checkPWLimits();

		if (mountPWx == MINPWx || mountPWx == MAXPWx)
			sweepLeft = !sweepLeft;

		scanPW[0] = mountPWx;
		scanPW[1] = mountPWy;

		return scanPW;
	}

	public void checkPWLimits() {
		if (mountPWx > MAXPWx) {
			mountPWx = MAXPWx;
			Log.d("Servo", "Went above max PWx");
		}
		if (mountPWx < MINPWx) {
			mountPWx = MINPWx;
			Log.d("Servo", "Went below min PWx");
		}

		if (mountPWy > MAXPWy) {
			mountPWy = MAXPWy;
			Log.d("Servo", "Went above max PWx");
		}
		if (mountPWy < MINPWy) {
			mountPWy = MINPWy;
			Log.d("Servo", "Went below min PWx");
		}
		// check bounds of forward-back motor
	}

	public class IRCalculations {
		boolean backingUp = false;
		boolean goingForward = false;
		double frontIRVoltage, leftIRVoltage, rightIRVoltage, lSideVoltage,
				rSideVoltage;

		public IRCalculations() {
		}

		public void calculateIR(boolean doBacking, boolean reverseWheels) {
			if (frontIRVoltage > 2.0) {
				doBacking = true;

				if (!reverseWheels) {
					double diff = wheelPW - MIDDLEPW; // pos if bigger, neg if
														// less
					wheelPW = MIDDLEPW - diff; // reverses the wheels
					motorPW = BACKMOTOR + 50; // goes a bit less than full back
					reverseWheels = true;
				}
			}
			if (doBacking) {
				motorPW = BACKMOTOR + 50; // check
				if (frontIRVoltage < 1.0) {
					doBacking = false;
					reverseWheels = false;
				}
			}
			Log.d("calculateIR", "Reading the voltage is" + frontIRVoltage);

			controller.doBacking = doBacking;
			controller.reverseWheels = reverseWheels;
		}

		public void calculateLeftRightIR() {
			boolean goingLeft = false;
			boolean goingRight = false;

			if (wheelPW < MIDWHEEL) // the mount is looking left, turn left
				goingLeft = true;
			else if (wheelPW > MIDWHEEL)
				goingRight = true;
			else // its looking in the middle
			{
				goingRight = false;
				goingLeft = false;
			}

			// difference between the side and the middle of the wheel
			int difference = WHEELMAX - WHEELMIN;
			double beginTurnVoltage = 1.0; // when the wheels just begin to turn
			double maxTurnVoltage = 2.0; // when the wheels should be turning at
											// maximum

			double changeInPWLeft = (leftIRVoltage / maxTurnVoltage)
					* difference;
			double changeInPWRight = (rightIRVoltage / maxTurnVoltage)
					* difference;

			if (leftIRVoltage > beginTurnVoltage)
				wheelPW = wheelPW + changeInPWLeft;
			if (rightIRVoltage > beginTurnVoltage)
				wheelPW = wheelPW - changeInPWRight;

			// DO A CHECK AT THE END
			wheelPWCheck();
		}

		public void calculateSideIR() {
			
			int difference = (WHEELMAX - WHEELMIN)/3;
			double beginTurnVoltage = 1.0; // when the wheels just begin to turn
			double maxTurnVoltage = 2.0; // when the wheels should be turning at
											// maximum

			double changeInPWLeft = (lSideVoltage / maxTurnVoltage)
					* difference;
			double changeInPWRight = (rSideVoltage / maxTurnVoltage)
					* difference;

			if (lSideVoltage > beginTurnVoltage)
				wheelPW = wheelPW + changeInPWLeft;
			if (rSideVoltage > beginTurnVoltage)
				wheelPW = wheelPW - changeInPWRight;


//			if (lSideVoltage > 1.0) {
//				wheelPW += wheelVeerLeftPW;
//			}
//
//			if (rSideVoltage > 1.0) {
//				wheelPW += wheelVeerRightPW;
//			}
			
			//Always check!
			wheelPWCheck();

		}

		public void setVoltage(float IRFront, float IRLeft, float IRRight,
				float IRLSide, float IRRSide) {
			frontIRVoltage = IRFront;
			leftIRVoltage = IRLeft;
			rightIRVoltage = IRRight;
			rSideVoltage = IRRSide;
			lSideVoltage = IRLSide;

			Log.d("setVoltage", "The front IR voltage is " + IRFront);
		}
	}
}