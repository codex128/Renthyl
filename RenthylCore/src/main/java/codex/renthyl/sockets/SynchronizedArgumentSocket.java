package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

public class SynchronizedArgumentSocket<T> extends ArgumentSocket<T> {

    private static final int PADDING = 5;

    private int[] schedule;
    private int nextPosition = Integer.MAX_VALUE;
    private int lastPosition = -1;

    public SynchronizedArgumentSocket(Renderable task) {
        super(task);
    }

    public SynchronizedArgumentSocket(Renderable task, T value) {
        super(task, value);
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return queuePosition == nextPosition && super.isAvailableToDownstream(queuePosition);
    }

    @Override
    public void resetSocket() {
        if (nextPosition <= lastPosition) {
            throw new IllegalStateException("Not all references were released.");
        }
        super.resetSocket();
    }

    @Override
    public void reference(int queuePosition) {
        if (schedule == null) {
            schedule = new int[queuePosition + PADDING];
        } else if (queuePosition >= schedule.length) {
            int[] temp = new int[queuePosition + PADDING];
            System.arraycopy(schedule, 0, temp, 0, schedule.length);
            schedule = temp;
        }
        schedule[queuePosition]++;
        nextPosition = Math.min(nextPosition, queuePosition);
        lastPosition = Math.max(lastPosition, queuePosition);
        super.reference(queuePosition);
    }

    @Override
    public void release(int queuePosition) {
        super.release(queuePosition);
        if (queuePosition != nextPosition) {
            throw new IllegalStateException("Release of synchronized socket is out of order.");
        }
        schedule[nextPosition]--;
        while (nextPosition <= lastPosition && schedule[nextPosition] <= 0) {
            nextPosition++;
        }
    }

}
