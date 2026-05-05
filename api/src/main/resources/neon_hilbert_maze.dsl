Config {
  lineLength: 52.0
  turningAngle: 90.0
  maxIterations: 7
  startingColor: cyan
}

Colors {
  cyan: 0.00, 1.00, 1.00
  magenta: 1.00, 0.00, 1.00
}

Axiom: A

Rules {
  A -> - <magenta> B F + <cyan> A F A + F B -
  B -> + <cyan> A F - <magenta> B F B - F A +
}