package com.wetterquarz.moveengine;
import java.util.Collection;
import java.util.LinkedList;

import org.bukkit.util.Vector;

public final class Path {
	
	private final Vector[] nodes;
	
	public Path(Collection<Vector> nodes) {
		this.nodes = new Vector[nodes.size()];
		nodes.toArray(this.nodes);
		generateInternodes();
	}
	
	private Vector2[] firstInternodes;
	private Vector2[] secondInternodes;
	private void generateInternodes() {
		int N = nodes.length-1;
		firstInternodes = new Vector2[N];
		var c = new double[N];
		
		/* Pass over the matrix (Thomas-Algorithm) */
		c[0] = 0.5;
		firstInternodes[0] = new Vector2();
		firstInternodes[0].x = (nodes[0].getX() + 2 * nodes[1].getX()) / 2.0;
		firstInternodes[0].y = (nodes[0].getZ() + 2 * nodes[1].getZ()) / 2.0;
		
		for(int n = 1 ; n < N ; n+=1) {
			double m = 1.0 / (b(n) - a(n) * c[n-1]);
			firstInternodes[n] = new Vector2();
			if(n < N-1) {
				c[n] = m;
				firstInternodes[n].x = (4*nodes[n].getX() + 2*nodes[n+1].getX() - a(n) * firstInternodes[n-1].x) * m;
				firstInternodes[n].y = (4*nodes[n].getZ() + 2*nodes[n+1].getZ() - a(n) * firstInternodes[n-1].y) * m;
			} else {
				firstInternodes[n].x = (8*nodes[n].getX() +   nodes[n+1].getX() - a(n) * firstInternodes[n-1].x) * m;
				firstInternodes[n].y = (8*nodes[n].getZ() +   nodes[n+1].getZ() - a(n) * firstInternodes[n-1].y) * m;
			}
		}
		
		/* Go back the other way and find the results */
		for(int n = N - 2 ; n >= 0 ; n-=1) {
			firstInternodes[n].x = firstInternodes[n].x - c[n] * firstInternodes[n+1].x;
			firstInternodes[n].y = firstInternodes[n].y - c[n] * firstInternodes[n+1].y;
		}
		
		/* Generate the correlating internode */
		secondInternodes = new Vector2[N];
		for(int n = 0 ; n < N-1 ; n+=1) {
			secondInternodes[n] = new Vector2();
			secondInternodes[n].x = 2*nodes[n+1].getX() - firstInternodes[n+1].x;
			secondInternodes[n].y = 2*nodes[n+1].getZ() - firstInternodes[n+1].y;
		}
		secondInternodes[N-1] = new Vector2();
		secondInternodes[N-1].x = (nodes[N].getX() + firstInternodes[N-1].x)/2.0;
		secondInternodes[N-1].y = (nodes[N].getZ() + firstInternodes[N-1].y)/2.0;
	}
		
	private double a(int n) {
		return n < nodes.length-2 ? 1 : 2;
	}

	private double b(int n) {
		return n < nodes.length-2 ? 4 : 7;
	}
	
	public int getSegments() {
		return nodes.length-1;
	}
	
	public Vector getPosition(int segment, double progress) {
		if(segment >= getSegments()) {
			return nodes[nodes.length-1];
		}
		double x = Math.pow(1-progress, 3) * nodes[segment].getX() + 3*(((progress-2)*progress+1)*progress) * firstInternodes[segment].x + 3*(-progress+1)*progress*progress * secondInternodes[segment].x + Math.pow(progress, 3)*nodes[segment+1].getX();
		double z = Math.pow(1-progress, 3) * nodes[segment].getZ() + 3*(((progress-2)*progress+1)*progress) * firstInternodes[segment].y + 3*(-progress+1)*progress*progress * secondInternodes[segment].y + Math.pow(progress, 3)*nodes[segment+1].getZ();
		double y = (1-progress) * nodes[segment].getY() + progress * nodes[segment+1].getY();
		return new Vector(x, y, z);
	}
	
	public Vector getOrientation(int segment, double progress) {
		double x = -3*Math.pow(1-progress, 2) * nodes[segment].getX() + 3*((3*progress-4)*progress+1) * firstInternodes[segment].x + 3*(-3*progress+2)*progress * secondInternodes[segment].x + 3*Math.pow(progress, 2)*nodes[segment+1].getX();
		double z = -3*Math.pow(1-progress, 2) * nodes[segment].getZ() + 3*((3*progress-4)*progress+1) * firstInternodes[segment].y + 3*(-3*progress+2)*progress * secondInternodes[segment].y + 3*Math.pow(progress, 2)*nodes[segment+1].getZ();
		var result = new Vector(x,0,z);
		result.normalize();
		return result;
	}
	
	public double get2DSegmentLength(int segment) {
		double a = nodes[segment].getX() - nodes[segment+1].getX();
		double b = nodes[segment].getZ() - nodes[segment+1].getZ();
		return Math.sqrt(a*a+b*b);
	}
	
	public LinkedList<Vector> getAllNodes() {
		var l = new LinkedList<Vector>();
		for(int i = 0 ; i < nodes.length - 1 ; i++) {
			l.add(nodes[i]);
			l.add(new Vector(firstInternodes[i].x, nodes[i].getY(), firstInternodes[i].y));
			l.add(new Vector(secondInternodes[i].x, nodes[i].getY(), secondInternodes[i].y));
		}
		l.add(nodes[nodes.length-1]);
		return l;
	}
	
}
