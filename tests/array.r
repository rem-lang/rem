@def static putchar(c: i32) i32

def main() {
  var a = [1, 2, 3]
  a[2] = 5

  for var i = 0; i < 3; i++ {
    putchar(48 + a[i])
  }
}
