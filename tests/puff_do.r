@def static putchar(c: i32) i32

def puff(a: i32) i32 {
  return a *= 5
}

def main() {
  putchar(91)

  var i = 97;
  do {
    putchar(i)
    i++
  } while i < 123

  putchar(93)
  putchar(10)
}
