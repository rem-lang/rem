@def static putchar(c: i32) i32

def puff(a: i32) i32 {
  return a *= 5
}

def main() {
  putchar(91)

  var i = 97;
  while i < 123 {
    putchar(i)
    i++
  }

  putchar(93)
  putchar(10)
}
