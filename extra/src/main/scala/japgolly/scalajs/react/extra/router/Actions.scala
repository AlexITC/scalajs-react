package japgolly.scalajs.react.extra.router

import japgolly.scalajs.react.vdom.VdomElement

// If we don't extend Product with Serializable here, a method that returns both a Renderer[P] and a Redirect[P] will
// be type-inferred to "Product with Serializable with Action[Page, Props]" which breaks the Renderable & Actionable implicits.
sealed trait Action[Page, -Props] extends Product with Serializable {
  def map[A](f: Page => A): Action[A, Props]
}

final case class RendererF[F[_], Page, -Props](render: RouterCtlF[F, Page] => Props => VdomElement) extends Action[Page, Props] {
  override def toString = s"Renderer($render)"
  @inline def apply(ctl: RouterCtlF[F, Page]): Props => VdomElement =
    render(ctl)

  override def map[A](g: Page => A): RendererF[F, A, Props] =
    RendererF(r => render(r contramap g))
}

sealed trait Redirect[Page] extends Action[Page, Any] {
  override def map[A](f: Page => A): Redirect[A]
}

final case class RedirectToPage[Page](page: Page, via: SetRouteVia) extends Redirect[Page] {
  override def map[A](f: Page => A): RedirectToPage[A] =
    RedirectToPage(f(page), via)
}

final case class RedirectToPath[Page](path: Path, via: SetRouteVia) extends Redirect[Page] {
  override def map[A](f: Page => A): RedirectToPath[A] =
    RedirectToPath(path, via)
}
