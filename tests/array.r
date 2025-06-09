@def static putchar(c: i32) i32
@def static puts(c: [4096]i8) i32

def contra(v: [4]i32) {
  for var i = 0; v[i] != 0; i++ {
    putchar(v[i])
  }
}

def main() {
  var a = [1, 2, 3]
  a[2] = 5

  for var i = 0; i < 3; i++ {
    putchar(48 + a[i])
  }

  var b = 10
  var c = 20
  var d = 25

  var e: [3]i8 = [b, c, d]

  for var i = 0; i < 3; i++ {
    putchar(64 + e[i])
  }

  var f = 65
  var g = 66
  var h = 67

  var e: [4]i8 = [f, g, h, 0]
  contra(e)
}
