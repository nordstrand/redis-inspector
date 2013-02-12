import org.junit.runner.RunWith;

import expectations.junit.ExpectationsTestRunner;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class ClojureTest implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        // return the path to your root test dir here
        return "src/test/clojure";
    }

}

