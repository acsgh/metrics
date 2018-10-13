package com.acs.metrics.core

import java.util.concurrent.TimeUnit

import com.codahale.metrics
import com.codahale.metrics.ExponentiallyDecayingReservoir

trait Metric {
  def values: Map[String, AnyVal]
}

sealed trait CleanAfterPublishMetric extends Metric

case class Counter() extends Metric with CleanAfterPublishMetric {

  private val counter = new metrics.Counter()

  def inc(delta: Long = 1): Unit = update(delta)

  def dec(delta: Long = 1): Unit = update(-1 * delta)

  def update(delta: Long): Unit = counter.inc(delta)

  override def values: Map[String, AnyVal] = Map("count" -> counter.getCount)
}

case class Timer() extends Metric with CleanAfterPublishMetric {

  private val timer = new metrics.Timer()

  def update(value: Long, unit: TimeUnit): Unit = timer.update(value, unit)

  override def values: Map[String, AnyVal] = Map()
}

case class Meter() extends Metric with CleanAfterPublishMetric {

  private val meter = new metrics.Meter()

  def mark(value: Long = 1): Unit = meter.mark(value)

  override def values: Map[String, AnyVal] = Map()
}

case class Histogram() extends Metric with CleanAfterPublishMetric {

  private val histogram = new metrics.Histogram(new ExponentiallyDecayingReservoir())

  def update(value: Long): Unit = histogram.update(value)

  override def values: Map[String, AnyVal] = {
    val snapshot = histogram.getSnapshot
    Map(
      "count" -> histogram.getCount
    "min" -> snapshot.getMin
    )
  }
}

abstract case class Gauge[T <: AnyVal]() extends Metric {

  def value: T

  override def values: Map[String, AnyVal] = Map("value" -> value)
}