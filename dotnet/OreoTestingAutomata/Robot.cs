using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace OreoTestingAutomata
{
    public class Robot
    {
        /// <summary>
        /// xxxx
        /// </summary>
        public static byte VK_LBUTTON = 0x01;
        public static byte VK_RBUTTON = 0x02;
        public static byte VK_CANCEL = 0x03;
        public static byte VK_MBUTTON = 0x04;
        public static byte VK_XBUTTON1 = 0x05;
        public static byte VK_XBUTTON2 = 0x06;
        public static byte VK_BACK = 0x08;
        public static byte VK_TAB = 0x09;
        public static byte VK_CLEAR = 0x0C;
        public static byte VK_RETURN = 0x0D;
        public static byte VK_SHIFT = 0x10;
        public static byte VK_CONTROL = 0x11;
        public static byte VK_MENU = 0x12;
        public static byte VK_PAUSE = 0x13;
        public static byte VK_CAPITAL = 0x14;
        public static byte VK_KANA = 0x15;
        public static byte VK_HANGUEL = 0x15;
        public static byte VK_HANGUL = 0x15;
        public static byte VK_IME_ON = 0x16;
        public static byte VK_JUNJA = 0x17;
        public static byte VK_FINAL = 0x18;
        public static byte VK_HANJA = 0x19;
        public static byte VK_KANJI = 0x19;
        public static byte VK_IME_OFF = 0x1A;
        public static byte VK_ESCAPE = 0x1B;
        public static byte VK_CONVERT = 0x1C;
        public static byte VK_NONCONVERT = 0x1D;
        public static byte VK_ACCEPT = 0x1E;
        public static byte VK_MODECHANGE = 0x1F;
        public static byte VK_SPACE = 0x20;
        public static byte VK_PRIOR = 0x21;
        public static byte VK_NEXT = 0x22;
        public static byte VK_END = 0x23;
        public static byte VK_HOME = 0x24;
        public static byte VK_LEFT = 0x25;
        public static byte VK_UP = 0x26;
        public static byte VK_RIGHT = 0x27;
        public static byte VK_DOWN = 0x28;
        public static byte VK_SELECT = 0x29;
        public static byte VK_PRINT = 0x2A;
        public static byte VK_EXECUTE = 0x2B;
        public static byte VK_SNAPSHOT = 0x2C;
        public static byte VK_INSERT = 0x2D;
        public static byte VK_DELETE = 0x2E;
        public static byte VK_HELP = 0x2F;
        public static byte VK_X_0 = 0x30;
        public static byte VK_X_1 = 0x31;
        public static byte VK_X_2 = 0x32;
        public static byte VK_X_3 = 0x33;
        public static byte VK_X_4 = 0x34;
        public static byte VK_X_5 = 0x35;
        public static byte VK_X_6 = 0x36;
        public static byte VK_X_7 = 0x37;
        public static byte VK_X_8 = 0x38;
        public static byte VK_X_9 = 0x39;
        public static byte VK_X_A = 0x41;
        public static byte VK_X_B = 0x42;
        public static byte VK_X_C = 0x43;
        public static byte VK_X_D = 0x44;
        public static byte VK_X_E = 0x45;
        public static byte VK_X_F = 0x46;
        public static byte VK_X_G = 0x47;
        public static byte VK_X_H = 0x48;
        public static byte VK_X_I = 0x49;
        public static byte VK_X_J = 0x4A;
        public static byte VK_X_K = 0x4B;
        public static byte VK_X_L = 0x4C;
        public static byte VK_X_M = 0x4D;
        public static byte VK_X_N = 0x4E;
        public static byte VK_X_O = 0x4F;
        public static byte VK_X_P = 0x50;
        public static byte VK_X_Q = 0x51;
        public static byte VK_X_R = 0x52;
        public static byte VK_X_S = 0x53;
        public static byte VK_X_T = 0x54;
        public static byte VK_X_U = 0x55;
        public static byte VK_X_V = 0x56;
        public static byte VK_X_W = 0x57;
        public static byte VK_X_X = 0x58;
        public static byte VK_X_Y = 0x59;
        public static byte VK_X_Z = 0x5A;
        public static byte VK_LWIN = 0x5B;
        public static byte VK_RWIN = 0x5C;
        public static byte VK_APPS = 0x5D;
        public static byte VK_SLEEP = 0x5F;
        public static byte VK_NUMPAD0 = 0x60;
        public static byte VK_NUMPAD1 = 0x61;
        public static byte VK_NUMPAD2 = 0x62;
        public static byte VK_NUMPAD3 = 0x63;
        public static byte VK_NUMPAD4 = 0x64;
        public static byte VK_NUMPAD5 = 0x65;
        public static byte VK_NUMPAD6 = 0x66;
        public static byte VK_NUMPAD7 = 0x67;
        public static byte VK_NUMPAD8 = 0x68;
        public static byte VK_NUMPAD9 = 0x69;
        public static byte VK_MULTIPLY = 0x6A;
        public static byte VK_ADD = 0x6B;
        public static byte VK_SEPARATOR = 0x6C;
        public static byte VK_SUBTRACT = 0x6D;
        public static byte VK_DECIMAL = 0x6E;
        public static byte VK_DIVIDE = 0x6F;
        public static byte VK_F1 = 0x70;
        public static byte VK_F2 = 0x71;
        public static byte VK_F3 = 0x72;
        public static byte VK_F4 = 0x73;
        public static byte VK_F5 = 0x74;
        public static byte VK_F6 = 0x75;
        public static byte VK_F7 = 0x76;
        public static byte VK_F8 = 0x77;
        public static byte VK_F9 = 0x78;
        public static byte VK_F10 = 0x79;
        public static byte VK_F11 = 0x7A;
        public static byte VK_F12 = 0x7B;
        public static byte VK_F13 = 0x7C;
        public static byte VK_F14 = 0x7D;
        public static byte VK_F15 = 0x7E;
        public static byte VK_F16 = 0x7F;
        public static byte VK_F17 = 0x80;
        public static byte VK_F18 = 0x81;
        public static byte VK_F19 = 0x82;
        public static byte VK_F20 = 0x83;
        public static byte VK_F21 = 0x84;
        public static byte VK_F22 = 0x85;
        public static byte VK_F23 = 0x86;
        public static byte VK_F24 = 0x87;
        public static byte VK_NUMLOCK = 0x90;
        public static byte VK_SCROLL = 0x91;
        public static byte VK_LSHIFT = 0xA0;
        public static byte VK_RSHIFT = 0xA1;
        public static byte VK_LCONTROL = 0xA2;
        public static byte VK_RCONTROL = 0xA3;
        public static byte VK_LMENU = 0xA4;
        public static byte VK_RMENU = 0xA5;
        public static byte VK_BROWSER_BACK = 0xA6;
        public static byte VK_BROWSER_FORWARD = 0xA7;
        public static byte VK_BROWSER_REFRESH = 0xA8;
        public static byte VK_BROWSER_STOP = 0xA9;
        public static byte VK_BROWSER_SEARCH = 0xAA;
        public static byte VK_BROWSER_FAVORITES = 0xAB;
        public static byte VK_BROWSER_HOME = 0xAC;
        public static byte VK_VOLUME_MUTE = 0xAD;
        public static byte VK_VOLUME_DOWN = 0xAE;
        public static byte VK_VOLUME_UP = 0xAF;
        public static byte VK_MEDIA_NEXT_TRACK = 0xB0;
        public static byte VK_MEDIA_PREV_TRACK = 0xB1;
        public static byte VK_MEDIA_STOP = 0xB2;
        public static byte VK_MEDIA_PLAY_PAUSE = 0xB3;
        public static byte VK_LAUNCH_MAIL = 0xB4;
        public static byte VK_LAUNCH_MEDIA_SELECT = 0xB5;
        public static byte VK_LAUNCH_APP1 = 0xB6;
        public static byte VK_LAUNCH_APP2 = 0xB7;
        public static byte VK_OEM_1 = 0xBA;
        public static byte VK_OEM_PLUS = 0xBB;
        public static byte VK_OEM_COMMA = 0xBC;
        public static byte VK_OEM_MINUS = 0xBD;
        public static byte VK_OEM_PERIOD = 0xBE;
        public static byte VK_OEM_2 = 0xBF;
        public static byte VK_OEM_3 = 0xC0;
        public static byte VK_OEM_4 = 0xDB;
        public static byte VK_OEM_5 = 0xDC;
        public static byte VK_OEM_6 = 0xDD;
        public static byte VK_OEM_7 = 0xDE;
        public static byte VK_OEM_8 = 0xDF;
        public static byte VK_OEM_102 = 0xE2;
        public static byte VK_PROCESSKEY = 0xE5;
        public static byte VK_PACKET = 0xE7;
        public static byte VK_ATTN = 0xF6;
        public static byte VK_CRSEL = 0xF7;
        public static byte VK_EXSEL = 0xF8;
        public static byte VK_EREOF = 0xF9;
        public static byte VK_PLAY = 0xFA;
        public static byte VK_ZOOM = 0xFB;
        public static byte VK_NONAME = 0xFC;
        public static byte VK_PA1 = 0xFD;
        public static byte VK_OEM_CLEAR = 0xFE;

        public class Position
        {
            public int I;
            public int J;
            public Position(int _i, int _j)
            {
                I = _i;
                J = _j;
            }
        }

        public class ScreenCapture
        {
            [DllImport("user32.dll")]
            private static extern IntPtr GetForegroundWindow();

            [DllImport("user32.dll", CharSet = CharSet.Auto, ExactSpelling = true)]
            public static extern IntPtr GetDesktopWindow();

            [StructLayout(LayoutKind.Sequential)]
            private struct Rect
            {
                public int Left;
                public int Top;
                public int Right;
                public int Bottom;
            }

            [DllImport("user32.dll")]
            private static extern IntPtr GetWindowRect(IntPtr hWnd, ref Rect rect);

            public static Image CaptureDesktop()
            {
                return CaptureWindow(GetDesktopWindow());
            }

            public static Bitmap CaptureActiveWindow()
            {
                return CaptureWindow(GetForegroundWindow());
            }

            public static int[,] CaptureActiveWindowPixels()
            {
                return Script.GetPixels(CaptureWindow(GetForegroundWindow()));
            }

            public static Bitmap CaptureWindow(IntPtr handle)
            {
                var rect = new Rect();
                GetWindowRect(handle, ref rect);
                var bounds = new Rectangle(rect.Left, rect.Top, rect.Right - rect.Left, rect.Bottom - rect.Top);
                var result = new Bitmap(bounds.Width, bounds.Height);

                using (var graphics = Graphics.FromImage(result))
                {
                    graphics.CopyFromScreen(new Point(bounds.Left, bounds.Top), Point.Empty, bounds.Size);
                }

                return result;
            }
        }

        public static void CopyStringToClipboard(string text)
        {
            System.Windows.Forms.Clipboard.SetText(text);
        }

        public static Position GetCurrentMousePosition()
        {
            var p = MouseOperations.GetCursorPosition();
            return new Position(p.Y, p.X);
        }

        internal static void SetMousePosition(int i, int j)
        {
            MouseOperations.SetCursorPosition(j, i);
        }

        internal static void ContinuousMoveTo(int i, int j, int millis)
        {
            /*var interval = 20;
            var current = GetCurrentMousePosition();
            double di = i - current.I;
            double dj = j - current.J;
            double dr = Math.Sqrt(di * di + dj * dj);
            double vr = interval * dr / millis;
            double vi = vr * di / Math.Abs(di);
            double vj = vr * dj / Math.Abs(dj);*/
            throw new NotImplementedException();
        }

        internal static void SetMousePosition(Position position)
        {
            MouseOperations.SetCursorPosition(position.J, position.I);
        }

        internal static void LeftClick()
        {
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.LeftDown);
            Thread.Sleep(200);
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.LeftUp);
        }

        internal static void LeftDown()
        {
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.LeftDown);
        }

        internal static void LeftUp()
        {
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.LeftUp);
        }

        internal static void RightClick()
        {
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.RightDown);
            Thread.Sleep(200);
            MouseOperations.MouseEvent(MouseOperations.MouseEventFlags.RightUp);
        }

        // http://www.kbdedit.com/manual/low_level_vk_list.html
        internal static void KeyPress(byte keyCode)
        {
            MouseOperations.KeyPress(keyCode);
        }
        internal static void KeyDown(byte keyCode)
        {
            MouseOperations.KeyDown(keyCode);
        }
        internal static void KeyUp(byte keyCode)
        {
            MouseOperations.KeyUp(keyCode);
        }

        internal static void Wait(int millis)
        {
            Thread.Sleep(millis);
        }

        public static void Enter()
        {
            KeyDown(VK_RETURN);
            Wait(100);
            KeyUp(VK_RETURN);
        }

        public static void Delete()
        {
            KeyDown(VK_DELETE);
            Wait(100);
            KeyUp(VK_DELETE);
        }

        public static void CtrlA()
        {
            KeyDown(VK_CONTROL);
            Wait(200);
            KeyDown(VK_X_A);
            Wait(100);
            KeyUp(VK_X_A);
            Wait(200);
            KeyUp(VK_CONTROL);
        }

        public static void CtrlC()
        {
            KeyDown(VK_CONTROL);
            Wait(200);
            KeyDown(VK_X_C);
            Wait(100);
            KeyUp(VK_X_C);
            Wait(200);
            KeyUp(VK_CONTROL);
        }

        public static void CtrlV()
        {
            KeyDown(VK_CONTROL);
            Wait(400);
            KeyDown(VK_X_V);
            Wait(200);
            KeyUp(VK_X_V);
            Wait(400);
            KeyUp(VK_CONTROL);
        }

        internal static void CtrlW()
        {
            KeyDown(VK_CONTROL);
            Wait(200);
            KeyDown(VK_X_W);
            Wait(100);
            KeyUp(VK_X_W);
            Wait(200);
            KeyUp(VK_CONTROL);
        }

        internal static void ESC()
        {
            KeyDown(VK_ESCAPE);
            Wait(100);
            KeyUp(VK_ESCAPE);
        }

        private class MouseOperations
        {
            [Flags]
            public enum MouseEventFlags
            {
                LeftDown = 0x00000002,
                LeftUp = 0x00000004,
                MiddleDown = 0x00000020,
                MiddleUp = 0x00000040,
                Move = 0x00000001,
                Absolute = 0x00008000,
                RightDown = 0x00000008,
                RightUp = 0x00000010
            }

            [DllImport("user32.dll", EntryPoint = "SetCursorPos")]
            [return: MarshalAs(UnmanagedType.Bool)]
            private static extern bool SetCursorPos(int x, int y);

            [DllImport("user32.dll")]
            [return: MarshalAs(UnmanagedType.Bool)]
            private static extern bool GetCursorPos(out MousePoint lpMousePoint);

            [DllImport("user32.dll")]
            private static extern void mouse_event(int dwFlags, int dx, int dy, int dwData, int dwExtraInfo);

            [DllImport("user32.dll", CharSet = CharSet.Auto, CallingConvention = CallingConvention.StdCall)]
            public static extern void keybd_event(uint bVk, uint bScan, uint dwFlags, uint dwExtraInfo);

            public static void SetCursorPosition(int x, int y)
            {
                SetCursorPos(x, y);
            }

            public static void SetCursorPosition(MousePoint point)
            {
                SetCursorPos(point.X, point.Y);
            }

            public static MousePoint GetCursorPosition()
            {
                var gotPoint = GetCursorPos(out MousePoint currentMousePoint);
                if (!gotPoint) { currentMousePoint = new MousePoint(0, 0); }
                return currentMousePoint;
            }

            public static void MouseEvent(MouseEventFlags value)
            {
                MousePoint position = GetCursorPosition();

                mouse_event
                    ((int)value,
                     position.X,
                     position.Y,
                     0,
                     0)
                    ;
            }

            public static void KeyPress(byte keyCode)
            {
                /*const int KEYEVENTF_EXTENDEDKEY = 0x1;
                const int KEYEVENTF_KEYUP = 0x2;
                keybd_event(keyCode, 0x45, KEYEVENTF_EXTENDEDKEY, 0);
                Thread.Sleep(200);
                keybd_event(keyCode, 0x45, KEYEVENTF_EXTENDEDKEY | KEYEVENTF_KEYUP, 0);*/

                uint KEYEVENTF_KEYUP = 0x0002;
                keybd_event(keyCode, 0, 0, 0);
                Thread.Sleep(100);
                keybd_event(keyCode, 0, KEYEVENTF_KEYUP, 0);
            }

            public static void KeyDown(byte keyCode)
            {
                keybd_event(keyCode, 0, 0, 0);
            }

            public static void KeyUp(byte keyCode)
            {
                keybd_event(keyCode, 0, 0x0002, 0);
            }

            [StructLayout(LayoutKind.Sequential)]
            public struct MousePoint
            {
                public int X;
                public int Y;

                public MousePoint(int x, int y)
                {
                    X = x;
                    Y = y;
                }
            }
        }
    }
}
