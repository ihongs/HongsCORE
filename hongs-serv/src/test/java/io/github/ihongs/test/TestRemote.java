package io.github.ihongs.test;

import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Remote.EventStream;
import java.io.IOException;
import java.util.Map;
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
            EventStream es = new EventStream() {
                public void accept(Map<String, String> map) {
                    System.out.println(Dist.toString(map));
                }
            };
        ) {
            for (String str : arr) {
                es.write(str.getBytes());
            }
        }
    }
    
}
