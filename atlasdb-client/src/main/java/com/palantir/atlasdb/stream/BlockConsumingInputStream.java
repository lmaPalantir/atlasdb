/**
 * Copyright 2016 Palantir Technologies
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.ArrayUtils;

public final class BlockConsumingInputStream extends InputStream {
    private final BiConsumer<Integer, OutputStream> blockGetter;
    private final long numBlocks;

    private int nextBlockToRead;
    private Iterator<Byte> buffer;

    public static BlockConsumingInputStream create(BiConsumer<Integer, OutputStream> blockGetter, long numBlocks)
            throws IOException {
        BlockConsumingInputStream stream = new BlockConsumingInputStream(blockGetter, numBlocks);
        stream.getNextBlock();
        return stream;
    }

    private BlockConsumingInputStream(BiConsumer<Integer, OutputStream> blockGetter, long numBlocks) {
        this.blockGetter = blockGetter;
        this.numBlocks = numBlocks;
        this.nextBlockToRead = 0;
    }

    @Override
    public int read() throws IOException {
        if (buffer.hasNext()) {
            return buffer.next() & 0xff;
        }

        if (nextBlockToRead < numBlocks) {
            getNextBlock();
            return buffer.next() & 0xff;
        }

        return -1;
    }

    private void getNextBlock() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.blockGetter.accept(nextBlockToRead, outputStream);
        byte[] bytes = outputStream.toByteArray();
        List<Byte> list = Arrays.asList(ArrayUtils.toObject(bytes));
        this.buffer = list.iterator();
        outputStream.close();
        nextBlockToRead++;
    }
}