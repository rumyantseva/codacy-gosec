package com.codacy.gosec

import com.codacy.analysis.core.model.IssuesAnalysis
import com.codacy.analysis.core.model.IssuesAnalysis.FileResults
import com.codacy.tool.ClientSideToolEngine

import scala.util.{Failure, Success, Try}

object Gosec extends ClientSideToolEngine(toolName = "gosec") {

  private def gosecReportToFileResults(report: GosecResult): Try[Set[FileResults]] = {
    Try {
      report.issues
        .groupBy(_.file)
        .view
        .map {
          case (path, res) =>
            FileResults(path, res.view.map(_.toCodacyIssue).toSet)
        }
        .to(Set)
    }
  }

  private def gosecReportToIssuesAnalysis(report: GosecResult): IssuesAnalysis = {
    gosecReportToFileResults(report) match {
      case Success(result) => IssuesAnalysis.Success(result)
      case Failure(exception) => IssuesAnalysis.Failure(exception.getMessage)
    }

  }

  override def convert(lines: Seq[String]): IssuesAnalysis = {
    GosecReportParser.fromJson(lines) match {
      case Right(report) => gosecReportToIssuesAnalysis(report)
      case Left(err) => IssuesAnalysis.Failure(err.getMessage)
    }
  }
}
