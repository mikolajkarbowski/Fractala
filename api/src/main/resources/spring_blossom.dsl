Config {
  turningAngle: 22.5
  maxIterations: 6
  lineLength: 150
}

Colors {
  wood: 0.6, 0.4, 0.2
  bloom: 1.0, 0.4, 0.7
}

Axiom: F

Rules {
  F (0.33) -> F [ + <bloom> F ] F
  F (0.33) -> F [ - <bloom> F ] F
  F (0.34) -> F <wood> F
}