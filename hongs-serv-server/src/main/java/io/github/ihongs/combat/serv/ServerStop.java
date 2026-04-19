package io.github.ihongs.combat.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.combat.anno.Combat;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 停止服务
 * @author Hongs
 */
@Combat("server.stop")
public class ServerStop {

    // libc API (Linux/MacOS)
    private interface Libc  extends Library {
        int kill(int pid, int sig);
    }

    // Windows API
    private interface Win32 extends Library {
        boolean GenerateConsoleCtrlEvent(int dwCtrlEvent, int dwProcessGroupId);
        boolean AttachConsole(int dwProcessId);
        boolean FreeConsole  ();
        int     GetLastError ();
    }

    private static final int SIGINT = 2; // Linux/MacOS/Windows 都代表 Ctrl+C
    private static final int CTRL_C_EVENT = 0;
    private static final int ATTACH_PARENT_PROCESS = -1;

    @Combat("__main__")
    public static void stop(String[] args) throws CruxException {
        String serd = Core.DATA_PATH + File.separator + "server" ;
        File   ppid = new  File(serd + File.separator + "ppid" ) ;

        if (ppid.exists() == false) {
            System.err.println("ERROR: The server has stopped.") ;
            return;
        }

        try {
            String   pidx = Files.readString(ppid.toPath());
            String[] pids = pidx . split( "\\s+", 2 );
            int      pid  = Integer.parseInt(pids[0]);

            if (Platform.isWindows()) {
                // Windows：JNA 调用系统 API
                Win32 win32 = Native.load("kernel32", Win32.class);
                try {
                    // 1. 先释放当前进程控制台（解决错误码 5）
                    win32.FreeConsole();
        
                    // 2. 附加到目标进程控制台 (解决错误码 87)
                    if (! win32.AttachConsole(pid)) {
                        System.err.println("ERROR: stop error! " + win32.GetLastError());
                        return;
                    }
                    
                    // 3. 关键：不能传 PID！传 0 表示当前控制台所有进程
                    if (! win32.GenerateConsoleCtrlEvent(CTRL_C_EVENT, 0)) {
                        System.err.println("ERROR: stop error: " + win32.GetLastError());
                    }
                } finally {
                    // 4. 分离控制台
                    win32.FreeConsole();
                }
            } else {
                // Linux/MacOS：JNA 直接调用 libc kill()
                Libc libc = Native.load("c", Libc.class);
                int  code = libc.kill ( pid, SIGINT );
                if ( code != 0 ) {
                    System.err.println("ERROR: stop error: " + code );
                    return;
                }
            }

            // 等待服务终止
            int i = 0;
            while (ppid.exists( )) {
                if (20 < i++) {
                    System.err.println("ERROR: stop timeout!");
                    return;
                }
                Thread.sleep(500L);
            }

            System.err.print("Server stopped!");
        }
        catch (IOException | InterruptedException ex) {
            throw new CruxException(ex);
        }

    }
}
