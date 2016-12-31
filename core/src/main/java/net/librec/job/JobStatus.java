/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.job;

/**
 * @author WangYuFeng
 */
public class JobStatus {

    public static final int RUNNING = 1;
    public static final int SUCCEEDED = 2;
    public static final int FAILED = 3;
    public static final int PREP = 4;

    private static final String UNKNOWN = "UNKNOWN";
    private static final String[] RUNSTATES = {UNKNOWN, "RUNNING", "SUCCEEDED", "FAILED", "PREP"};

    private String jobId;
    private String jobStage;
    private float progress;
    private long startTime;
    private long finishTime;

    /**
     * Helper method to get human-readable state of the job.
     *
     * @param state job state
     * @return human-readable state of the job
     */
    public static String getJobRunState(int state) {
        if (state < 1 || state >= RUNSTATES.length) {
            return UNKNOWN;
        }
        return RUNSTATES[state];
    }

    /**
     * @return the jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * @param jobId the jobId to set
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the jobStage
     */
    public String getJobStage() {
        return jobStage;
    }

    /**
     * @param jobStage the jobStage to set
     */
    public void setJobStage(String jobStage) {
        this.jobStage = jobStage;
    }

    /**
     * @return the progress
     */
    public float getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(float progress) {
        this.progress = progress;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the finishTime
     */
    public long getFinishTime() {
        return finishTime;
    }

    /**
     * @param finishTime the finishTime to set
     */
    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }
}
