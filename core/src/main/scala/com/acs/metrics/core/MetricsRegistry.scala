package com.acs.metrics.core

import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.acs.metrics.core.MetricsRegistry._

import scala.collection.mutable

object MetricsRegistry {

  sealed trait MetricMessage

  case class MetricKey(name: String, tags: Map[String, AnyVal] = Map())

  case class CounterUpdate(key: MetricKey, value: Long = 1) extends MetricMessage

  case class TimerUpdate(key: MetricKey, value: Long, unit: TimeUnit) extends MetricMessage

  case class HistogramUpdate(key: MetricKey, value: Long) extends MetricMessage

  case class RegisterMetric(key: MetricKey, metric: Metric) extends MetricMessage

  case class PublishMetrics() extends MetricMessage

  case class PublishMetric(key: MetricKey, timeStamp: Instant, values: Map[String, AnyVal])

}

class MetricsRegistry(metricPublisher: ActorRef) extends Actor with ActorLogging {

  private var metrics = mutable.Map[MetricKey, Metric]()

  override def receive: Receive = {
    case RegisterMetric(key, metric) => registerMetricIfNotPresent(key, metric)
    case CounterUpdate(key, value) => registerMetricIfNotPresent(key, Counter()).update(value)
    case TimerUpdate(key, value, unit) => registerMetricIfNotPresent(key, Timer()).update(value, unit)
    case HistogramUpdate(key, value) => registerMetricIfNotPresent(key, Histogram()).update(value)
    case PublishMetrics() =>
      val storeMetrics = metrics ++ Map()
      storeMetrics.foreach(e => metricPublisher ! PublishMetric(e._1, Instant.now(), e._2.values))
      metrics.retain((_, value) => !value.isInstanceOf[CleanAfterPublishMetric])
    case _ => throw new RuntimeException("Unable to process message")
  }

  private def registerMetricIfNotPresent[T <: Metric](key: MetricKey, metric: T): T = {
    metrics.get(key).fold({
      metrics += (key -> metric)
      metric
    }) {
      case t: T =>
        t
      case storedMetric =>
        throw new IllegalArgumentException(s"Metric $key is already register as ${storedMetric.getClass.getSimpleName}")
    }
  }
}
