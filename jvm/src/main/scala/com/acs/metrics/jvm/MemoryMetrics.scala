package com.acs.metrics.jvm

import com.acs.metrics.core.Metric
import com.codahale.metrics.jvm.MemoryUsageGaugeSet
import com.codahale.metrics.{Gauge, MetricSet}

import scala.collection.JavaConverters._

trait JVMMetrics extends Metric {
  val provider: MetricSet

  override def values: Map[String, AnyVal] = provider.getMetrics.asScala
    .filter(_._2.isInstanceOf[Gauge[Any]])
    .mapValues(v => get(v.asInstanceOf[Gauge[Any]]))
    .filter(_._2.isDefined)
    .mapValues(_.get)
    .toMap

  private def get(gauge: Gauge[Any]): Option[AnyVal] = Some(gauge.asInstanceOf[Gauge[Any]].getValue).filter(_.isInstanceOf[AnyVal]).map(_.asInstanceOf[AnyVal])
}

class MemoryMetrics extends JVMMetrics {
  override val provider = new MemoryUsageGaugeSet()
}
