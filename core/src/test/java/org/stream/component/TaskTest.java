import org.stream.extension.meta.Task;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TaskTest {

    @Test
    public void testParse() {
        Task task = Task.parse(null);
        Assert.assertNull(task);
    }
}