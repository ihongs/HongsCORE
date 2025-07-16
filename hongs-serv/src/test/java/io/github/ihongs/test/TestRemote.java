package io.github.ihongs.test;

import io.github.ihongs.util.Remote.EventStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestRemote {

    @Test
    public void testEventStream() throws IOException {
        String[] arr = new String[] {
            "event: start\n",
            "data: Hello\n",
            "data: World\n",
            "\n",
            "data: Who are you?\n",
            "\n\n",
            "data: How are you!\n",
            "\n\n\n",
            ": Comments\n",
            "\n",
            "event: end\n",
            "\n"
        };

        List<String> rel = new ArrayList<>();
        List<String> rst = new ArrayList<>();

        rel.add("Hello\nWorld");
        rel.add("Who are you?");
        rel.add("How are you!");
        rel.add("end");

        try (
            EventStream es = new EventStream()
                .on("start"  , dat -> rst.add( dat ))
                .on("message", dat -> rst.add( dat ))
                .on("end"    , dat -> rst.add("end"));
        ) {
            for (String str : arr) {
                es.write(str.getBytes());
            }
        }

        assertEquals(rel, rst);
    }

}
