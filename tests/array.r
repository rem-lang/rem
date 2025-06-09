@def static putchar(c: i32) i32

def main() {
  var a = [1, 2, 3]
  a[2] = 5

  for var i = 0; i < 3; i++ {
    putchar(48 + a[i])
  }

  var b = 10
  var c = 20
  var d = 25

  var e: []i8 = [b, c, d]

  for var i = 0; i < 3; i++ {
    putchar(64 + e[i])
  }
}
