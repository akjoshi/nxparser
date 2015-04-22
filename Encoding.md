# Encoding/Escaping #

We do  a lot of processing of Nx data, taking Nx as input and Nx as output (Nx meaning 7-bit ASCII). Hence, all constructors and internal objects store/buffer Nx escaped strings by default to avoid the expense of parsing/encoding/decoding. Equals, hashcode, etc., is all based on Nx-strings.

When decoded data is requested (quite rare; mainly in UIs by my guess), it is parsed from the Nx string and decoded.
As such, the whole package is setup to **always** deal internally with Nx  strings. Strings should be escaped when passed through the Node constructors. Static methods are provided in NxUtils to escape/unescape to and from such strings.

Non-static methods to unescape are given to Node using toString(). toN3() should always return an Nx valid string
representing the object, complete with delimiters. That's all we should need.