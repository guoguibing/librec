package net.librec.job.progress;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProgressBar {

    private double finishPoint;
    private double barLength;
    private SimpleDateFormat formatter = new SimpleDateFormat(" MM-dd HH:mm:ss");


    public ProgressBar() {
        this.finishPoint = 100;
        this.barLength = 20;
    }

    public ProgressBar(double finishPoint, int barLength) {
        this.finishPoint = finishPoint;
        this.barLength = barLength;
    }

    /**
     * 显示进度条
     *
     * @param currentPoint 当前点
     */
    @Test
    public void showBarByPoint(double currentPoint) {
        // 根据进度参数计算进度比率
        double rate = currentPoint / this.finishPoint;
        // 根据进度条长度计算当前记号
        int barSign = (int) (rate * this.barLength);
        // 生成进度条
        System.out.println(makeBarBySignAndLength(barSign) + String.format(" %.2f%%", rate * 100) + " " + formatter.format(new Date()));
    }

    /**
     * 构造进度条
     *
     * @param barSign 进度条标记(当前点)
     * @return 字符型进度条
     */
    private String makeBarBySignAndLength(int barSign) {
        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 1; i <= this.barLength; i++) {
            if (i < barSign) {
                bar.append("-");
            } else if (i == barSign) {
                bar.append(">");
            } else {
                bar.append(" ");
            }
        }
        bar.append("]");
        return bar.toString();
    }

    public static void main(String[] args) {
        ProgressBar pb = new ProgressBar(100, 100);
        for (int i = 0; i <= 100; i += 20) {
            pb.showBarByPoint(i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}