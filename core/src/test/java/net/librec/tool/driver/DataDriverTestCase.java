package net.librec.tool.driver;

import net.librec.BaseTestCase;
import org.junit.Test;

/**
 * Created by syt on 2016/11/12.
 */
public class DataDriverTestCase extends BaseTestCase {

    /**
     * Test the option "-build".
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        // D:/temp/pmf-test.properties
        //../data/test/pmf-test.properties
        String args[] = {"-build", "-D", "x1=111", "-jobconf", "x2=222", "-conf", "D:/temp/pmf-test.properties"};
        DataDriver.main(args);
    }
}
