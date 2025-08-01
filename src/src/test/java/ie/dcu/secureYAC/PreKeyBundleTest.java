package ie.dcu.secureYAC;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import java.io.File;

public class PreKeyBundleTest {
    @Test
    public void testExport() throws Exception {
        User test = new User("test", 50);
        PreKeyBundle prekey = test.getPreKeyBundle();
        prekey.export();
        for (File f : new File(".").listFiles()) {
            if (f.getName().endsWith(".pkb")) {
                f.delete();
                return;
            }
        }
        fail(".pkb file not found.");
    }

    @Test
    public void testMultiExport() throws Exception {
        User test = new User("test", 50);
        PreKeyBundle prekey = test.getPreKeyBundle();
        for (int i = 10; i != 0; i--) {
            prekey.export();
        }
        int counter = 0;
        for (File f : new File(".").listFiles()) {
            if (f.getName().endsWith(".pkb")) {
                counter += 1;
                f.delete();
            }
        }
        assertEquals(10, counter);
    }
}
