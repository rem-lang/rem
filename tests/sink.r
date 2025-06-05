var name: i32 = 20

name = name + 12
name += 91
name = 45

class Vec {
  @new() {}
}

class Map {}
class String < Vec {}

var ages: []Vec = []

def test() {

}

def test2() i32 {
  return 10
}

def test3(name: [i8]Vec) {

}

def test4(age: i32) bool {
  return false
}

class Person {
  @new(test: [i16][i32][i8]Vec) {

  }

  method() {

  }

  method2() f64 {
    return 1.48374893647836496783567846378946937846
  }

  method3(age: i8) {

  }

  method4(address: f128) Person {
    return new Person({
      1: {
        2: {
          3: new Vec()
        }
      }
    })
  }
}

class James < Person {
  var age: i32 = 25
}

@(param: i32) {

}(25)

var games: [i8]i32 = {
  1: name
}

var nums: []i8 = [1, 2, 3, 4, 5]
var nums: []i32 = [10, 20, 30, 40]
var nums: []bool = [true, false]


var s: Vec = new Vec()
var s: Vec = new String()