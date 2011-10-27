/**
 * 
 */
package edu.ncsu.hmalipa.csc570.project1;

import java.util.*;
import javax.swing.*;
import org.math.plot.*;
import org.math.plot.plotObjects.*;
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * @author harsha
 * 
 */
public class CsmaCdSimulation {
	private static Logger logger = Logger.getLogger(CsmaCdSimulation.class);
	private int A = 0;
	private int B = 1;
	private double CLOCK = 0;

	private double TOTALSIM = 60d * Math.pow(10, 6); // Total simulation time
	private float lambda = 0.5f;
	private int frameslot = 500; // frame slot time (usec)
	private int td = 80;// transmission delay on BUS (usec)
	private int pd = 10; // propagation delay on BUS
	private int tdelay = td + pd; // total delay incurred during a pkt
									// transmission

	private ArrayList<Packet> AtoB = new ArrayList<Packet>();
	private ArrayList<Packet> BtoA = new ArrayList<Packet>();
	private ArrayList<Packet> elist = new ArrayList<Packet>();
	private ArrayList<Packet> SIMRESULT = new ArrayList<Packet>();

	private double[] GENTIMECURSOR;

	private Random rand = new Random();
	
	private void run() {
		logger.debug("Starting simulation");
		GENTIMECURSOR = new double[] { 0d, 0d };

		createpacket(A);
		createpacket(B);

		logger.debug("Updating clock");
		updateclock();
		logger.debug("updated clock value is " + (float)CLOCK);
		if (elist.size() == 0) {
			disp("No packets to simulate");
			return;
		}

		while (true) {
			logger.debug("Updating clock");
			updateclock();
			logger.debug("updated clock value is " + (float)CLOCK);
			//updateclock();
			Packet p = elist.get(0);

			if (p.INITIALTXTIME == 0) {
				logger.debug("Packet " + p + ": is contending for link first time");
				logger.debug("Updating initial txtime");
				p.INITIALTXTIME = p.CURTIME;
				logger.debug("Updated packet debugrmation " + p);
			}

			/*
			 * If the transmission time of the two packets is less than
			 * propagation delay then backof sending of each packet and then
			 * continue.
			 */
			if ((elist.get(1).CURTIME - elist.get(0).CURTIME) < pd) {
				if (elist.get(1).INITIALTXTIME == 0) {
					//logger.debug("Packet " + p + ": is contending for link first time");
					//logger.debug("Updating initial txtime");
					elist.get(1).INITIALTXTIME = elist.get(1).CURTIME;
					//logger.debug("Updated packet debugrmation " + p);
				}
				logger.error("collision occured");
				logger.debug(elist);
				elist.get(0).CURTIME = elist.get(1).CURTIME + pd;
				elist.get(1).CURTIME = elist.get(0).CURTIME + pd;
				logger.debug("Updated current time for each packet");
				logger.debug(elist);
				logger.debug("Backing off");
				backoff();
				logger.debug("Updated elist is");
				logger.debug(elist);
				continue;
			}

			logger.debug("No collision occured");
			int src = p.SRC;

			p.TXTIME = p.CURTIME;

			p.RXTIME = p.TXTIME + tdelay;

			logger.info("Packet sent");
			logger.info(p);
			
			logger.debug("Updating simlist");
			updatesimlist();
			//logger.debug(SIMRESULT);

			//logger.debug("Creating new packet for src : " + ( src == A ? "A" : "B"));
			createpacket(src);

			logger.debug("Deferring all the packets current time who were trying to send when channel was busy");
			delaypkts(tdelay);
			logger.debug("Updated elist after adding delay");
			logger.debug(elist);

			if (GENTIMECURSOR[A] > TOTALSIM || GENTIMECURSOR[B] > TOTALSIM) {
				disp("Completed!");
				logger.debug("Simulation completed");
				calcstat();
				break;
			}
		}
	}

	private void backoff() {
		// Update number of collisions and
		elist.get(0).COLLISIONS++;
		elist.get(1).COLLISIONS++;
		double randomInteger1 = (min(
				 Math.pow(2, elist.get(0).COLLISIONS), 8d))*(rand.nextDouble());
		double randomInteger2 = (min(
				 Math.pow(2, elist.get(1).COLLISIONS), 8d))*(rand.nextDouble());

		elist.get(0).CURTIME = elist.get(0).CURTIME + randomInteger1 * frameslot;
		elist.get(1).CURTIME = elist.get(1).CURTIME + randomInteger2 * frameslot;
	}

	private double min(double d, double e) {
		if (d < e) {
			return d;
		}
		return e;
	}

	private void calcstat() {
		Iterator<Packet> itr = SIMRESULT.iterator();
		while (itr.hasNext()) {
			Packet p = itr.next();
			if (p.SRC == A) {
				AtoB.add(p);
			} else {
				assert (p.SRC == B);
				BtoA.add(p);
			}
		}

		int AtoBnum = AtoB.size();
		int BtoAnum = BtoA.size();

		double[] y = new double[BtoA.size()];
		for (int i = 0; i < BtoA.size(); ++i) {
			y[i] = i + 1d;
		}
		double[] yminus1 = new double[BtoA.size() - 1];
		for (int i = 0; i < BtoA.size() - 1; ++i) {
			yminus1[i] = i + 1d;
		}

		double[] x = new double[AtoB.size()];
		for (int i = 0; i < AtoB.size(); ++i) {
			x[i] = i + 1d;
		}
		double[] xminus1 = new double[AtoB.size() - 1];
		for (int i = 0; i < AtoB.size() - 1; ++i) {
			xminus1[i] = i + 1d;
		}

		double[] queuedelaya = new double[AtoB.size()];
		for (int i = 0; i < AtoB.size(); ++i) {
			Packet p = AtoB.get(i);
			queuedelaya[i] = p.TXTIME - p.GENTIME;
			// AtoB(:,TXTIME)-AtoB(:,GENTIME);
		}

		double[] queuedelayB = new double[BtoA.size()];
		for (int i = 0; i < BtoA.size(); ++i) {
			Packet p = BtoA.get(i);
			queuedelayB[i] = p.TXTIME - p.GENTIME;
			// AtoB(:,TXTIME)-AtoB(:,GENTIME);
		}

		// BaseLabel title;
		String xLabel = "Packet sequence #";
		String yLabelSuffix = String.valueOf("\u03BC") + " sec";
		Plot2DPanel queuedelayplot = plotPanelQueueDelay(AtoBnum, x,
				queuedelaya, xLabel, "Queue delay " + yLabelSuffix,
				"Queue delay at Node A");
		Plot2DPanel queuedelayplotB = plotPanelQueueDelay(BtoAnum, y,
				queuedelayB, xLabel, "Queue delay " + yLabelSuffix,
				"Queue delay at Node B");

		double[] accessdelaya = getAccessDelay(AtoB);
		double[] accessdelayb = getAccessDelay(BtoA);

		Plot2DPanel accessdelayaplot = panelPlotAccessDelay("A", AtoBnum, x,
				accessdelaya);
		Plot2DPanel accessdelaybplot = panelPlotAccessDelay("B", BtoAnum, y,
				accessdelayb);

		double[] frameintabA = getFrameDelay(AtoB);
		double[] frameintabB = getFrameDelay(BtoA);

		Plot2DPanel frameintabplotA = panelPlotFrameInterval("A", AtoBnum,
				xminus1, frameintabA);
		Plot2DPanel frameintabplotB = panelPlotFrameInterval("B", BtoAnum,
				yminus1, frameintabB);

		Plot2DPanel frameintabhistA = plotPanelFrameHist("A", frameintabA);
		Plot2DPanel frameintabhistB = plotPanelFrameHist("B", frameintabB);
		double[] endtoenda = getEndToEndDelay(AtoB);
		double[] endtoendb = getEndToEndDelay(BtoA);

		double meanendtoenda = mean(endtoenda);
		double meanendtoendb = mean(endtoendb);

		// Average end to end throughput
		//double Avgtha = ((1000d * 8d) / meanendtoenda) * Math.pow(10, 6); // bits/sec
		//double Avgthb = ((1000d * 8d) / meanendtoendb) * Math.pow(10, 6); // bits/sec
		double Avgtha = (1 / meanendtoenda) * Math.pow(10, 6); // bits/sec
		double Avgthb = (1 / meanendtoendb) * Math.pow(10, 6); // bits/sec

		printResults("A", AtoBnum, queuedelaya, accessdelaya, frameintabA,
				Avgtha);
		System.out.println("--------------------------------------");
		printResults("B", BtoAnum, queuedelayB, accessdelayb, frameintabB,
				Avgthb);
		fprintf("Simulation end time=//d\n", CLOCK);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
		}
		JFrame frame = new JFrame("Project ECE-570 Demo");
		frame.getContentPane().setLayout(new java.awt.GridLayout(4, 1));
		frame.getContentPane().add(queuedelayplot);
		frame.getContentPane().add(accessdelayaplot);
		frame.getContentPane().add(frameintabplotA);
		frame.getContentPane().add(frameintabhistA);

		frame.getContentPane().add(queuedelayplotB);
		frame.getContentPane().add(accessdelaybplot);
		frame.getContentPane().add(frameintabplotB);
		frame.getContentPane().add(frameintabhistB);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
		frame.setVisible(true);
	}

	private void printResults(String node, int AtoBnum, double[] queuedelaya,
			double[] accessdelaya, double[] frameintab, double Avgtha) {
		String other;
		other = (node == "A" ? "B" : "A");

		fprintf("Total packets sent from node " + node + "=//d\n",
				(float) AtoBnum);
		fprintf("Average frame interval at node " + node + "=//d\n",
				mean(frameintab));
		fprintf("Average access delay at node " + node + "=//d\n",
				mean(accessdelaya));
		fprintf("Average queue delay at node " + node + "=//d\n",
				mean(queuedelaya));
		fprintf("Average end to end throughput from " + node + " to " + other
				+ "=//d\n", Avgtha);
	}

	private double[] getEndToEndDelay(ArrayList<Packet> AtoB) {
		double[] endtoenda = new double[AtoB.size()];
		for (int i = 0; i < AtoB.size(); ++i) {
			Packet p = AtoB.get(i);
			endtoenda[i] = p.RXTIME - p.GENTIME;
		}
		return endtoenda;
	}

	private Plot2DPanel plotPanelFrameHist(String node, double[] frameintab) {
		Plot2DPanel frameintabhist = new Plot2DPanel();
		frameintabhist.addHistogramPlot("Frame intervals at node " + node,
				frameintab, 60);
		frameintabhist.setAxisLabels(
				"Frame interval in " + String.valueOf("\u03BC") + " sec",
				"# of frames");
		return frameintabhist;
	}

	private Plot2DPanel panelPlotFrameInterval(String node, int direction,
			double[] indVariable, double[] depVariable) {
		BaseLabel title;
		Plot2DPanel frameintabplot = new Plot2DPanel();
		frameintabplot.addBarPlot("Frame intervals at node " + node,
				indVariable, depVariable);
		frameintabplot.setAxisLabels("Packet sequence #",
				"Frame interval in msec");
		title = new BaseLabel("Frame intervals at node " + node,
				java.awt.Color.BLACK, 0.5d, 1.1d);
		frameintabplot.addPlotable(title);
		frameintabplot.setFixedBounds(0, 0d, (double) direction - 1d);
		frameintabplot.setFixedBounds(1, 0d, max(depVariable));
		return frameintabplot;
	}

	private double[] getFrameDelay(ArrayList<Packet> direction) {
		// Frame interval
		double[] frameintab = new double[direction.size() - 1];
		for (int i = 0; i < direction.size() - 1; ++i) {
			Packet p = direction.get(i);
			Packet p2 = direction.get(i + 1);
			frameintab[i] = (p2.GENTIME - p.GENTIME);
		}
		return frameintab;
	}

	private Plot2DPanel panelPlotAccessDelay(String node, int direction,
			double[] indVariable, double[] depVariable) {
		BaseLabel title;
		Plot2DPanel accessdelayaplot = new Plot2DPanel();
		accessdelayaplot.addLinePlot("Access delay at node " + node,
				indVariable, depVariable);
		accessdelayaplot.setAxisLabels("Packet sequence #", "Delay in "
				+ String.valueOf("\u03BC") + " sec");
		title = new BaseLabel("Access delay at node " + node,
				java.awt.Color.BLACK, 0.5d, 1.1d);
		accessdelayaplot.addPlotable(title);
		accessdelayaplot.setFixedBounds(0, 0d, (double) direction);
		accessdelayaplot.setFixedBounds(1, 0d, max(depVariable) + 5);
		return accessdelayaplot;
	}

	private double[] getAccessDelay(ArrayList<Packet> direction) {
		double[] accessdelaya = new double[direction.size()];
		for (int i = 0; i < direction.size(); ++i) {
			Packet p = direction.get(i);
			accessdelaya[i] = p.RXTIME - p.INITIALTXTIME;
		}
		return accessdelaya;
	}

	private Plot2DPanel plotPanelQueueDelay(int Direction,
			double[] indVaraibles, double[] depVariables, String xLabel,
			String yLabel, String titleName) {
		Plot2DPanel queuedelayplot = new Plot2DPanel();
		queuedelayplot.addBarPlot(titleName, indVaraibles, depVariables);
		queuedelayplot.setAxisLabels(xLabel, yLabel);
		BaseLabel title = new BaseLabel(titleName, java.awt.Color.BLACK, 0.5d,
				1.1d);
		queuedelayplot.addPlotable(title);
		queuedelayplot.setFixedBounds(0, 0d, (double) Direction);
		queuedelayplot.setFixedBounds(1, 0d, max(depVariables));
		return queuedelayplot;
	}

	// The clock slips to the RXTIME i.e., add delay time to CLOCK.
	private void delaypkts(int delay) {
		CLOCK = CLOCK + delay;
		// It might so happen that the new packet at the SRC node might have
		// been generated when the previous packet was in flight. This new
		// packet cannot be transmitted immediately and hence has to wait
		// till the previos packet has reached the destination.

		// list will have the row number of elist whose CURTIME field value
		// is less than CLOCK. Remember CLOCK is now the RXTIME.
		// list=find(((elist(:,CURTIME)-CLOCK) < 0));
		// Set the CURTIME field of all the rows in list to CLOCK.
		// elist(list,CURTIME)=CLOCK;

		for (Packet packet : elist) {
			if ((packet.CURTIME - CLOCK) < 0) {
				packet.INITIALTXTIME = packet.CURTIME;
				packet.CURTIME = CLOCK;
			}
		}
	}

	private void updateclock() {
		// SORTROWS(elist,CURTIME) sorts the rows of elist in ascending
		// order for the column CURTIME.
		// elist=sortrows(elist,CURTIME);
		logger.debug("sorting elist based on current time");
		Collections.sort(elist, new Comparator<Packet>() {
			public int compare(Packet o1, Packet o2) {
				return (o1.CURTIME < o2.CURTIME) ? -1
						: (o1.CURTIME == o2.CURTIME) ? 0 : 1;
			}

			public boolean equals(Object obj) {
				return this == obj;
			}
		});
		
		// Set the clock to the CURTIME of the packet in the first row of
		// the elist since this is the packet that contends first for the
		// channel.
		// CLOCK=elist(1,CURTIME);
		logger.debug("Next packet that is contending for channel is " + elist.get(0));
		CLOCK = elist.get(0).CURTIME;
	}

	// If the number of arrivals in any given time interval [0,t] follows
	// the Poisson distribution, with mean = ?t, then the lengths of the
	// inter-arrival times follow the Exponential distribution, with mean
	// 1/?. We can create packets all at once, i.e, keep creating packets
	// until the cummulative sum of the inter-arrival time is greater than
	// TOTALSIM (60 seconds). You might have around 10^4 packets generated
	// and all these packets will go into the event list. However if think
	// carefully, only two packets contend for the channel access at any
	// time, these are the packets at the head of the queue in node A and
	// node B. So you can avoid having other packets in the event list.
	// Hence a smarter way is to create a new packet at a node soon
	// after the packet at the head of the queue has left the node. However
	// you must be care keep track of the cummulative sum of inter-arrival
	// time upto the previous packet (the packet that just left the queue)
	// inorder to calculate the birth time of the packet generated.

	// Note: You can either create all packets at once or follow the
	// approach I have taken. It is totally upto you. The outcome will be
	// same with both the approaches will lead to the same outcome.
	private Packet createpacket(int nodeid) {
		// Find the inter-arrival time.
		//logger.debug("Creating packet for node " + (nodeid == 0 ? "A" : "B"));
		double x = (-1 / lambda) * (Math.log(1 -rand.nextDouble()));
		//double x = -Math.log(1 - .nextDouble()) / lambda;
		//double interarvtime = round(frameslot*exprnd(1/lambda,1,1));
		double interarvtime = Math.round(frameslot * x);

		logger.fatal("Inter arrival time is " + interarvtime);
		// Find the birth time.
		GENTIMECURSOR[nodeid] = GENTIMECURSOR[nodeid] + interarvtime;

		logger.debug("Birth time " + GENTIMECURSOR[nodeid]);
		// Create the packet. Unknown fields are set to 0.
		// [SRC= nodeid GENTIME=birthtime TXTIME=0, RXTIME=0
		// CURTIME=birthtime COLLISIONS=0]
		// pkt=[nodeid GENTIMECURSOR(nodeid) 0 0 GENTIMECURSOR(nodeid) 0];
		Packet packet = new Packet(nodeid, GENTIMECURSOR[nodeid],
				0, 0, GENTIMECURSOR[nodeid], 0);
		logger.debug("Packet details : " + packet);
		// Enqueue to the event list. In matlab you can append a row x to a
		// matrix X by using the command X=[X; x]
		// elist=[elist; pkt];
		elist.add(packet);
		return packet;
	}

	// Move the first row of elist to SIMRESULT
	private void updatesimlist() {
		SIMRESULT.add(elist.remove(0));
	}

	// disp(toc);

	private class Packet {
		public int SRC;
		public double GENTIME;
		public double TXTIME = 0;
		public double INITIALTXTIME = 0;
		public double RXTIME = 0;
		public double CURTIME;
		public int COLLISIONS = 0;

		public Packet(int src, double gentime, double txtime, double rxtime,
				double curtime, int collisions) {
			this.SRC = src;
			this.GENTIME = gentime;
			this.TXTIME = txtime;
			this.RXTIME = rxtime;
			this.CURTIME = curtime;
			this.COLLISIONS = collisions;
		}

		public String toString() {
			return "Queue delay: " + (TXTIME - GENTIME) +  " Access delay: " + (RXTIME - INITIALTXTIME) + "\n" + "SRC:" + SRC + ", GENTIME:" + (float)GENTIME + ", TXTIME:" + (float)TXTIME
					+ ", INITIALTXTIME:" + (float)INITIALTXTIME + ", RXTIME:" + (float)RXTIME
					+ ", CURTIME:" + (float)CURTIME + ", COLLISIONS:" + (float)COLLISIONS; 
		}
	}

	private double max(double[] vals) {
		double max = Double.MIN_VALUE;
		for (double d : vals) {
			max = Math.max(max, d);
		}
		return max;
	}

	private double mean(double[] vals) {
		double total = 0d;
		for (int i = 0; i < vals.length; ++i) {
			total += vals[i];
		}
		return total / (double) vals.length;
	}

	private void disp(String msg) {
		System.out.println(msg);
	}

	private void fprintf(String str, Object... args) {
		str = str.replaceAll("//d", "%f");
		System.out.print(String.format(str, args));
	}

	public static void main(String[] args) {
		DOMConfigurator.configure("log4j.xml");
		(new CsmaCdSimulation()).run();
	}
}
