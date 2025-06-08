@def static putchar(c: i32) i32

def puff(a: i32) i32 {
  return a *= 5
}

def main() {
  putchar(puff(18) + 1)

  for var i = 0; i < 26; i++ {
    if i == 10 break
    else if i == 3 or i == 6 continue

    putchar(65 + i)
  }

  putchar(puff(18) + 3)
  putchar(10)
}
