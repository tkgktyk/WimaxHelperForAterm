package jp.tkgktyk.wimaxhelperforaterm;

import jp.tkgktyk.wimaxhelperforaterm.AtermHelper.Product;

/**
 * An extra class of Router.
 * This represents that this application is unsupported the router.
 * Default implement is Aterm WM3800R.
 */
public class AtermUnsupported extends AtermWM3800R {
	@Override
	public Product toProduct() { return Product.UNSUPPORTED; }
}
