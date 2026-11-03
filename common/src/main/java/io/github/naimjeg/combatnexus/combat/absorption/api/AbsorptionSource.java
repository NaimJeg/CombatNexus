package io.github.naimjeg.combatnexus.combat.absorption.api;

import io.github.naimjeg.combatnexus.combat.api.CombatSource;

public record AbsorptionSource(CombatSource source, double capacity, double balance, double granted, double consumed) {}
