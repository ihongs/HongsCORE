package io.github.ihongs.test;

import io.github.ihongs.util.Remote.EventStream;
import java.io.IOException;
import org.junit.Test;

public class TestRemote {
    
    //@Test
    public void testEventStream() throws IOException {
        String[] arr = new String[] {
            "event: start\n",
            "data: Hello\n",
            "data: World\n",
            "\n",
            "data: Who are you?\n",
            "\n",
            "data: How are you!\n",
            "\n",
            "event: end\n"/*,
            "\n"*/
        };
        
        try (
            EventStream es = new EventStream()
                .on("start"  , dat -> System.out.println( dat ))
                .on("message", dat -> System.out.println( dat ))
                .on("end"    , dat -> System.out.println("end"));
        ) {
            for (String str : arr) {
                es.write(str.getBytes());
            }
        }
    }
    
}
