// Copyright (c) 2010-2013 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion.impl.lite;

import com.amazon.ion.IonClob;
import com.amazon.ion.IonException;
import com.amazon.ion.IonType;
import com.amazon.ion.IonWriter;
import com.amazon.ion.ValueVisitor;
import com.amazon.ion.impl._Private_Utils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 *
 */
final class IonClobLite
    extends IonLobLite
    implements IonClob
{
    static private final int HASH_SIGNATURE =
        IonType.CLOB.toString().hashCode();

    /**
     * Constructs a <code>null.clob</code> element.
     */
    public IonClobLite(IonSystemLite system, boolean isNull)
    {
        super(system, isNull);
    }

    @Override
    public IonClobLite clone()
    {
        IonClobLite clone = new IonClobLite(this._context.getSystem(), false);

        clone.copyFrom(this);

        return clone;
    }

    @Override
    public int hashCode() {
        return lobHashCode(HASH_SIGNATURE);
    }

    @Override
    public IonType getType()
    {
        return IonType.CLOB;
    }


    public Reader newReader(Charset cs)
    {
        InputStream in = newInputStream();
        if (in == null) return null;

        return new InputStreamReader(in, cs);
    }


    public String stringValue(Charset cs)
    {
        // TODO use Charset directly.
        byte[] bytes = getBytes();
        if (bytes == null) return null;

        return _Private_Utils.decode(bytes, cs);
    }

    public final void writeTo(IonWriter writer) {
        try {
            writer.setTypeAnnotationSymbols(getTypeAnnotationSymbols());
            if (isNullValue()) {
                writer.writeNull(IonType.CLOB);
            } else {
                writer.writeClob(getBytesNoCopy());
            }
        } catch (Exception e) {
            throw new IonException(e);
        }
    }

    @Override
    public void accept(ValueVisitor visitor) throws Exception
    {
        visitor.visit(this);
    }
}
