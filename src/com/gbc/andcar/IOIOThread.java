package com.gbc.andcar;
import android.util.Log;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;

public class IOIOThread extends BaseIOIOLooper 
{
		private PwmOutput pwmOutputx;
		private PwmOutput pwmOutputy;
		private PwmOutput motorOutput;
		private PwmOutput wheelOutput;
		private DigitalOutput led_;
		Controller the_gui;
		 //600 TO 2500 
		
		boolean goingBackwards;
		
		int lastMotorPW;
		double[] PWs = new double[4];
		
		//IRs
		private AnalogInput IRFront, IRLeft, IRRight, IRRSide, IRLSide;

		//passes in a reference to sample2view FROM sample2nativecamera. bad programming?
		public IOIOThread(Controller viewScreen)
		{
			the_gui = viewScreen;

			Thread.currentThread().setName("IOIOThread");
			Log.d("IOIOThread", "IOIOThread has been created");

		}
		
		@Override
		public void setup() throws ConnectionLostException 
		{
			try {
				Log.d("IOIOThread", "Trying to finish setup of IOIO");
				double[] info = the_gui.servos.getSetupInfo();
				double PWx = info[0];
				double PWy = info[1];
				double motorPW = info[2];
				double wheelPW = info[3];
				
				pwmOutputx = ioio_.openPwmOutput(11, 100);
				pwmOutputy = ioio_.openPwmOutput(12, 100);
				motorOutput = ioio_.openPwmOutput(5, 100);
				wheelOutput = ioio_.openPwmOutput(10,100);
				IRFront = ioio_.openAnalogInput(43);
				IRLeft = ioio_.openAnalogInput(42);
				IRRight = ioio_.openAnalogInput(41);
				IRRSide = ioio_.openAnalogInput(40);
				IRLSide = ioio_.openAnalogInput(44);
				
				pwmOutputx.setPulseWidth((int)  PWx);
				pwmOutputy.setPulseWidth((int)  PWy); 
				motorOutput.setPulseWidth((int) motorPW);
				wheelOutput.setPulseWidth((int) wheelPW);
				the_gui.servos.irc.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());

				}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			finally
			{
				Log.d("IOIO_Tread", "IOIO thread sucessfully set up");
			}
		}
		
		public void loop() throws ConnectionLostException, InterruptedException
		{	
			the_gui.servos.irc.setVoltage(IRFront.getVoltage(), IRLeft.getVoltage(), IRRight.getVoltage(), IRRSide.getVoltage(), IRLSide.getVoltage());
			PWs = the_gui.servos.getServoPW();
			 int PWx = (int) PWs[0];
			 int PWy = (int) PWs[1];
			 int motorPW = (int) PWs[2];
			 int wheelPW = (int) PWs[3];

			pwmOutputx.setPulseWidth(PWx);
			pwmOutputy.setPulseWidth(PWy);
			wheelOutput.setPulseWidth(wheelPW);

			
			//for going between backwards and forwards
			//WARNING: MINIMIZE GOING FROM FULL SPEED FORWARD TO FULL SPEED BACKWARDS
			//DOING SO CAN DAMAGE THE GEARS
			if (motorPW <  ServoCalculations.ACTUALSTOP)
				motorOutput.setPulseWidth(ServoCalculations.ACTUALSTOP);
			
//			if (!goingBackwards && (motorPW < ServoCalculations.ACTUALSTOP))
//			{
//					motorOutput.setPulseWidth(ServoCalculations.ACTUALSTOP);
//					Thread.sleep(20); //so it gets a chance to get up to speed
//					goingBackwards = true;
//			}
//			
//			if (motorPW >= ServoCalculations.ACTUALSTOP)
//				goingBackwards = false;
			
			motorOutput.setPulseWidth((int) motorPW);
			lastMotorPW = (int) motorPW;

			Thread.sleep(50);
		}
	}
