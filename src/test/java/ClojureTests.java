import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

import clojure.main;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class ClojureTests implements ExpectationsTestRunner.TestSource{

    public String testPath() {
        // return the path to your root test dir here
        return "src/test/clojure";
    }
    
  
    public static void main(String[] args) {
		System.out.println("compiled");
	}
}

