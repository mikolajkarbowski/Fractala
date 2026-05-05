Config {
  lineLength: 80.0
  lineWidth: 2.0
  turningAngle: 25.7
  maxIterations: 6
  startingColor: bark
}

Colors {
  bark: 0.2, 0.2, 0.3
  crystalLight: 0.6, 0.9, 1.0
  crystalDark: 0.1, 0.5, 0.9
}

Axiom: F

Rules {
  F (0.4) -> <bark> F [ + F ] F [ - F ] [ F ]
  F (0.3) -> <bark> F [ + <crystalLight> F ] F [ - F ] [ F ]
  F (0.3) -> <bark> F [ + F ] F [ - <crystalDark> F ] [ F ]
}