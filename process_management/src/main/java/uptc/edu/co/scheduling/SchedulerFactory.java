package uptc.edu.co.scheduling;

public class SchedulerFactory {
    public static ProcessScheduler createScheduler(SchedulerType type, Object... parameters) {
        switch (type) {
            case ROUND_ROBIN:
                int quantum = parameters.length > 0 ? (Integer) parameters[0] : 4;
                return new RoundRobinScheduler(quantum);
                
            case SJF:
                boolean preemptiveSJF = parameters.length > 0 ? (Boolean) parameters[0] : false;
                return new SJFScheduler(preemptiveSJF);
                
            case PRIORITY:
                boolean preemptivePriority = parameters.length > 0 ? (Boolean) parameters[0] : true;
                return new PriorityScheduler(preemptivePriority);
                
            default:
                throw new IllegalArgumentException("Scheduler no supported: " + type);
        }
    }
    
    public enum SchedulerType {
        ROUND_ROBIN, SJF, PRIORITY
    }
}