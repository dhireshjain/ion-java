// Copyright (c) 2010-2011 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion.impl.lite;

import static com.amazon.ion.impl.UnifiedSymbolTable.isNonSystemSharedTable;

import com.amazon.ion.IonDatagram;
import com.amazon.ion.IonSystem;
import com.amazon.ion.SymbolTable;
import com.amazon.ion.impl.UnifiedSymbolTable;


/**
 *
 */
final class IonConcreteContext
    implements IonContext
{
    /**
     * Do we need this?  We should be able to follow
     * the _owning_context up until we get to a system.
     * And getSystem should not be called very often.
     */
    private final IonSystemLite _system;

    /**
     * Currently the _owning_context will be the system for a
     * loose value or a datagram when the top level value has
     * a local symbol table.  This could change is other
     * state starts being tracked in the context such as the
     * location in the binary buffer.
     * <p>
     * This is never null when this context is in use.
     * However it IS null when {@link #clear()}ed for recycling by
     * {@link IonSystemLite#releaseConcreteContext(IonConcreteContext)}.
     * <p>
     * TODO what if this is an interior context?
     * Does the point to the IonContainer?
     */
    private IonContext _owning_context;

    /**
     * This will be a local symbol table.  It is not valid
     * for this to be a shared symbol table since shared
     * symbol tables are only shared.  It will not be a
     * system symbol table as the system object will be
     * able to resolve its symbol table to the system
     * symbol table and following the parent/owning_context
     * chain will lead to a system object.
     * <p>
     * TODO ION-258 we cannot assume that the IonSystem knows the proper IVM
     * in this context
     */
    private SymbolTable _symbols;

    private IonConcreteContext(IonSystemLite system,
                               IonContext owner)
    {
        _system = system;
        _owning_context = owner;
    }

    static IonConcreteContext wrap(IonSystemLite system,
                                   IonContext owner,
                                   IonValueLite child)
    {
        IonConcreteContext concrete = new IonConcreteContext(system, owner);

        child._context = concrete;

        return concrete;
    }

    static IonConcreteContext wrap(IonSystemLite system,
                                   SymbolTable symbols,
                                   IonValueLite child)
    {
        if (isNonSystemSharedTable(symbols)) {
            throw new IllegalArgumentException("you can only set a symbol table to a system or local table");
        }

        IonConcreteContext concrete = new IonConcreteContext(system, system);
        concrete._symbols = symbols;

        child._context = concrete;

        return concrete;
    }


    void rewrap(IonContext owner, IonValueLite child)
    {
        assert owner instanceof IonDatagramLite || owner == _system;
        _owning_context = owner;
        child._context = this;
    }


    private static boolean test_symbol_table_compatibility(IonContext parent,
                                                           IonValueLite child)
    {
        SymbolTable parent_symbols = parent.getSymbolTable();
        SymbolTable child_symbols = child.getAssignedSymbolTable();

        if (UnifiedSymbolTable.isLocalAndNonTrivial(child_symbols)) {
            // we may have a problem here ...
            if (child_symbols != parent_symbols) {
                // perhaps we should throw
                // but for now we're just ignoring this since
                // in a valueLite all symbols have string values
                // we could throw or return false
            }
        }
        return true;
    }

    protected void clear()
    {
        _owning_context = null;
        _symbols = null;
    }

    public void clearLocalSymbolTable()
    {
        _symbols = null;
    }

    public SymbolTable getLocalSymbolTable(IonValueLite child)
    {
        SymbolTable local;

        assert _owning_context != null;

        if (_symbols != null && _symbols.isLocalTable()) {
            local = _symbols;
        }
        //else if (_owning_context != null) {
        //    local = _owning_context.getLocalSymbolTable(child);
        //}
        else {
            IonSystem system = getSystem();
            local = system.newLocalSymbolTable();
            _symbols = local;
        }
        assert(local != null);

        return local;
    }

    public IonContainerLite getParentThroughContext()
    {
        // A concrete context only exists on a top level value.
        // Its parent should be a system or a datagram.

        if (_owning_context instanceof IonDatagramLite) {
            return (IonDatagramLite)_owning_context;
        }

        assert(_owning_context instanceof IonSystem);
        return null;
    }

    public SymbolTable getSymbolTable()
    {
        assert _owning_context != null;

        if (_symbols != null) {
            return _symbols;
        }
        if (_owning_context != null) {
            return _owning_context.getSymbolTable();
        }
        return _system.getSystemSymbolTable();
    }

    public SymbolTable getContextSymbolTable()
    {
        return _symbols;
    }

    public IonSystemLite getSystem()
    {
        assert(_system != null);
        return _system;
    }

    /**
     * @param container must not be null
     */
    public void setParentThroughContext(IonValueLite child,
                                        IonContainerLite container)
    {
        assert child._context == this;
        assert _owning_context instanceof IonSystemLite;

        // HACK: we need to refactor this to make it simpler and take
        //       away the need to check the parent type

        // but for now ...
        if (container instanceof IonDatagram)
        {
            // Leave this context between the TLV and the datagram, using the
            // same symbol table we already have.

            _owning_context = container;
        }
        else {
            // Some other container (struct, list, sexp, templist) is taking
            // over, this context is no longer needed.

            assert(test_symbol_table_compatibility(container, child));

            // FIXME this should be recycling this context
            // TODO this assumes there's never >1 value with the same context
            ((IonConcreteContext)child._context).clear();

            child.setContext(container);
        }
    }

    public void setSymbolTableOfChild(SymbolTable symbols, IonValueLite child)
    {
        assert (_owning_context instanceof IonSystem
             || _owning_context instanceof IonDatagram
        );

        // the only valid cases where you can set a concrete
        // contexts symbol table is when this is a top level
        // value.  That is the owning context is null, a datagram
        // of a system intance.

        if (isNonSystemSharedTable(symbols)) {
            throw new IllegalArgumentException("you can only set a symbol table to a system or local table");
        }
        _symbols = symbols;
        if (child._context != this) {
            assert(child._context != null && child._context == _owning_context);
            child._context = this;
        }
    }
}
