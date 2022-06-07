package japgolly.scalajs.react.test.emissions.util

import japgolly.microlibs.utils.FileUtils
import japgolly.univeq._
import java.util.regex.Pattern
import scala.util.matching.Regex

class MutableVirtualFile(val filenameOption: Option[String], val originalContent: String) {
  import MutableVirtualFile.StringFilterDsl

  var content = originalContent

  def changed(): Boolean =
    content !=* originalContent

  def set(newContent: String): this.type = {
    content = newContent
    this
  }

  def modify(f: String => String): this.type =
    set(f(content))

  def modifyLines(f: String => String): this.type =
    modifyLinesIterator(_.map(f))

  def modifyLinesIterator(f: Iterator[String] => Iterator[String]): this.type =
    set(f(content.linesIterator).mkString("\n") + "\n")

  def +=(newLine: String): this.type =
    set(content + "\n" + newLine)

  def dropLines(n: Int): this.type =
    modifyLinesIterator(_.drop(n))

  def dropLinesTo: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.dropWhile(f)))

  def dropLinesUntil: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.dropWhile(!f(_))))

  def dropLinesWhile: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.dropWhile(f)))

  def filter: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.filter(f)))

  def filterNot: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.filterNot(f)))

  def takeLines(n: Int): this.type =
    modifyLinesIterator(_.take(n))

  def takeLinesTo: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.takeWhile(delayMatchBy1(f))))

  def takeLinesUntil: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.takeWhile(!f(_))))

  def takeLinesWhile: StringFilterDsl[this.type] =
    StringFilterDsl(f => modifyLinesIterator(_.takeWhile(f)))

  private def delayMatchBy1[A](f: A => Boolean): A => Boolean = {
    var allow = true
    a => {
      if (allow && f(a)) {
        allow = false
        true
      } else
        allow
    }
  }

  def reset(): Unit =
    content = originalContent

  def trim(): Unit =
    set(content.trim + "\n")
}

object MutableVirtualFile {

  class FromFile(val filename: String, originalContent: String) extends MutableVirtualFile(Some(filename), originalContent) {
    def this(filename: String) =
      this(filename, Util.needFileContent(filename))

    def writeFile(): Unit =
      FileUtils.write(filename, content)

    def writeFileIfChanged(): Unit =
      if (changed()) writeFile()
  }

  final case class StringFilterDsl[A](private val run: (String => Boolean) => A) {

    def apply(f: String => Boolean): A =
      run(f)

    def apply(r: Regex): A =
      apply(r.pattern)

    def apply(p: Pattern): A =
      apply(p.matcher(_).matches())
  }
}