package com.example.slabiak.appointmentscheduler.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DayPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    private TimePeroid workingHours;
    private List<TimePeroid> breaks;

    public DayPlan() {
        breaks = new ArrayList();
    }

    public List<TimePeroid> timePeroidsWithBreaksExcluded() {
        ArrayList<TimePeroid> timePeroidsWithBreaksExcluded = new ArrayList<>();
        timePeroidsWithBreaksExcluded.add(getWorkingHours());
        List<TimePeroid> breaks = getBreaks();

        if (!breaks.isEmpty()) {
            ArrayList<TimePeroid> toAdd = new ArrayList();
            for (TimePeroid break1 : breaks) {
                if (break1.getStart().isBefore(workingHours.getStart())) {
                    break1.setStart(workingHours.getStart());
                }
                if (break1.getEnd().isAfter(workingHours.getEnd())) {
                    break1.setEnd(workingHours.getEnd());
                }
                for (TimePeroid peroid : timePeroidsWithBreaksExcluded) {
                    if (break1.getStart().equals(peroid.getStart()) && break1.getEnd().isAfter(peroid.getStart()) && break1.getEnd().isBefore(peroid.getEnd())) {
                        peroid.setStart(break1.getEnd());
                    }
                    if (break1.getStart().isAfter(peroid.getStart()) && break1.getStart().isBefore(peroid.getEnd()) && break1.getEnd().equals(peroid.getEnd())) {
                        peroid.setEnd(break1.getStart());
                    }
                    if (break1.getStart().isAfter(peroid.getStart()) && break1.getEnd().isBefore(peroid.getEnd())) {
                        toAdd.add(new TimePeroid(peroid.getStart(), break1.getStart()));
                        peroid.setStart(break1.getEnd());
                    }
                }
            }
            timePeroidsWithBreaksExcluded.addAll(toAdd);
            Collections.sort(timePeroidsWithBreaksExcluded);
        }


        return timePeroidsWithBreaksExcluded;
    }

    public TimePeroid getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(TimePeroid workingHours) {
        this.workingHours = workingHours;
    }

    public List<TimePeroid> getBreaks() {
        return breaks;
    }

    public void setBreaks(List<TimePeroid> breaks) {
        this.breaks = breaks;
    }

    public void removeBreak(TimePeroid breakToRemove) {
        breaks.remove(breakToRemove);
    }

    public void addBreak(TimePeroid breakToAdd) {
        breaks.add(breakToAdd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DayPlan dayPlan)) return false;
        return Objects.equals(workingHours, dayPlan.workingHours) &&
                Objects.equals(breaks, dayPlan.breaks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workingHours, breaks);
    }

}

