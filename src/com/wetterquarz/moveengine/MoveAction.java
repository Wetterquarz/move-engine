package com.wetterquarz.moveengine;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bukkit.util.Vector;

public class MoveAction {
	
	private static final LinkedList<MoveAction> actions = new LinkedList<>();
	
	private final Path path;
	private final Movable object;
	private final double blocksPerTick;
	private final Runnable callback;
	
	private double currentSegmentLength;
	private int currentSegment = 0;
	private double currentSegmentProgress = 0.0;
	private boolean finished = false;
	
	public MoveAction(Movable object, Path path) {
		this(object, path, null);
	}
	
	public MoveAction(Movable object, Path path, Runnable callback) {
		this(object, path, callback, 0.15);
	}
	
	public MoveAction(Movable object, Path path, Runnable callback, double blocksPerTick) {
		this.path = path;
		this.object = object;
		this.callback = callback;
		this.blocksPerTick = blocksPerTick;
	}
	
	public MoveAction(Movable object, Collection<Vector> path) {
		this(object, new Path(path));
	}
	
	public MoveAction(Movable object, Collection<Vector> path, Runnable callback) {
		this(object, new Path(path), callback);
	}
	
	public MoveAction(Movable object, Collection<Vector> path, Runnable callback, double blocksPerTick) {
		this(object, new Path(path), callback, blocksPerTick);
	}
	
	public void start() {
		currentSegmentLength = path.get2DSegmentLength(0);
		synchronized (actions) {
			actions.add(this);
			System.out.println("Action started");
		}
	}

	public void finish() {
		if(finished) return;
		synchronized (actions) {
			actions.remove(this);
		}
		callback.run();
	}
	
	public void cancel() {
		if(finished) return;
		synchronized (actions) {
			finished = true;
			actions.remove(this);
		}
	}
	
	private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor();
	static {
		WORKER.scheduleAtFixedRate(() -> {
			LinkedList<MoveAction> calls = new LinkedList<>();
			synchronized (actions) {
				for(var action : actions) {
					int n = action.path.getSegments();
					double movement = action.blocksPerTick;
					while(movement > 0) {
						action.currentSegmentProgress += movement / action.currentSegmentLength;
						movement = 0;
						if(action.currentSegmentProgress > 1.0) {
							System.out.println("Finished Segment");
							movement = (action.currentSegmentProgress - 1.0) * action.currentSegmentLength;
							action.currentSegmentProgress = 0;
							action.currentSegment+=1;
							if(action.currentSegment >= n) {
								System.out.println("Last Segment");
								break;
							}
							action.currentSegmentLength = action.path.get2DSegmentLength(action.currentSegment);
							System.out.println("Segment "+action.currentSegment);
						}
					}
					action.object.moveTo(action.path.getPosition(action.currentSegment, action.currentSegmentProgress), action.path.getOrientation(action.currentSegment, action.currentSegmentProgress));
				}
				actions.removeIf(action -> {
					int n = action.path.getSegments();
					if(action.currentSegment >= n) {
						action.finished = true;
						if(action.callback != null) calls.add(action);
						return true;
					}
					return false;
				});
			}
			calls.forEach(action -> action.callback.run());
		}, 50, 50, TimeUnit.MILLISECONDS);
		System.out.println("Scheduled");
	}
	
	/*public static void finishAll() {
		
	}
	
	public static void cancelAll() {
		
	}*/
	
}
