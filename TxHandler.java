package com.princeton.cryptocurrency.scroogeCoin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TxHandler {

	private UTXOPool utxoPoolcp;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
	public TxHandler(UTXOPool utxoPool) {
		if (utxoPool == null) {
			throw new IllegalArgumentException();
		}

        // IMPLEMENT THIS
    		this.utxoPoolcp = new UTXOPool(utxoPool);

	}

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS

    		double outputSum = 0;
    		for (int i=0; i<tx.numOutputs(); i++) {
    			Transaction.Output op = tx.getOutput(i);
    			// (4)
    			if (op.value <0) {
    				return false;
    			}

    			// (1)
    			//UTXO utxo = new UTXO(tx.getHash(), i);


    			outputSum+=op.value;
    		}

    		double inputSum = 0;
    		Set<UTXO> claimed = new HashSet<>();
    		for (int i=0; i<tx.numInputs(); i++) {
    			Transaction.Input ip = tx.getInput(i);

    			UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);
    			// (3)
    			if (!claimed.contains(utxo)) {
    				claimed.add(utxo);
    			} else {
    				return false;
    			}

    			// (1)
    			if (!utxoPoolcp.contains(utxo)) {
    				return false;
    			}

    			// (2)
    			Transaction.Output prevoutput = utxoPoolcp.getTxOutput(utxo);
    			if (!Crypto.verifySignature(prevoutput.address, tx.getRawDataToSign(i), ip.signature)) {
    				return false;
    			}


    			inputSum+=utxoPoolcp.getTxOutput(utxo).value;
    		}

    		// (5)
    		if (inputSum < outputSum) {
    			return false;
    		}

    		return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
    		// the output (generated from previous transaction) was used from this transaction
    		Set<UTXO> inputTracker = new HashSet<>();
		List<Transaction> doubleSpendCheckedValidTxs = new ArrayList<>();
    		for (Transaction tx : possibleTxs) {
    			// remove old unspend
    			boolean flag = true;
    			for (Transaction.Input ip : tx.getInputs()) {
    				UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);

    				// check against transactions in this round
    				if (!inputTracker.contains(utxo)) {
    					inputTracker.add(utxo);
    				} else {
    					inputTracker.remove(utxo);
    					flag = false;
    				}
    			}
    			if (flag) {
    				doubleSpendCheckedValidTxs.add(tx);
    			}
    		}

    		List<Transaction> validTxs = new ArrayList<>();
    		// now we have a set of double-spend checked transactions
    		for (Transaction tx :  doubleSpendCheckedValidTxs) {
    			if (isValidTx(tx)) {
    				// remove newly spended
    				for (Transaction.Input ip : tx.getInputs()) {
    					UTXO utxo = new UTXO(ip.prevTxHash, ip.outputIndex);
    		    			utxoPoolcp.removeUTXO(utxo);
    				}

    				// add newly generated unspend
    				for (int i=0; i<tx.numOutputs(); i++) {
    					UTXO newGeneratedUTXO = new UTXO(tx.getHash(), i);
    					utxoPoolcp.addUTXO(newGeneratedUTXO, tx.getOutput(i));
    				}
    				validTxs.add(tx);
    			}
    		}

    		return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
